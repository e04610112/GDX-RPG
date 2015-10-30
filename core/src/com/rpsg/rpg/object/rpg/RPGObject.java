package com.rpsg.rpg.object.rpg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.rpsg.rpg.core.RPG;
import com.rpsg.rpg.core.Setting;
import com.rpsg.rpg.object.rpg.Balloon.BalloonType;
import com.rpsg.rpg.system.base.Res;
import com.rpsg.rpg.system.ui.Image;
import com.rpsg.rpg.utils.game.Logger;

public abstract class RPGObject extends Actor implements Comparable<RPGObject>,Serializable{
	
	private static final long serialVersionUID = 1L;
	public static final int FACE_L=7;
	public static final int FACE_R=10;
	public static final int FACE_U=4;
	public static final int FACE_D=1;
	
	private static final int NORMAL_WALK_SPEED=4;
	
	transient Balloon bon=new Balloon(BalloonType.心);
	public transient Image[] images;
	
	public int lastWalkSize,lastZ,lastFace;
	public int layer;
	public int mapx,mapy;
	public Vector2 position =new Vector2();
	
	public boolean walked=true;
	
	public RPGObject(){}
	
	public int currentImageNo=1;
	
	public int foot=0;
	
	public boolean waitWhenCollide=true,enableCollide=true; 
	private boolean displayBalloon=false;
	
	public Collide collide=new Collide();
	
	public String imgPath;
	public int bodyWidth,bodyHeight;
	public List<MoveStack> walkStack=new ArrayList<MoveStack>(); 
	
	public boolean drawShadow = false;
	static Image shadow=Res.get(Setting.IRPGOBJECT_SHADOW);
	
	
	
	public Image getCurrentImage(){
		images[getCurrentFoot()].setPosition(position.x, position.y);
		return images[getCurrentFoot()];
	}
	
	@Override
	public int compareTo(RPGObject i) {
		if(i.mapy>this.mapy)
			return -1;
		else if(i.mapy==this.mapy)
			if(i.mapx>this.mapx)
				return -1;
			else if(i.mapx==this.mapx)
				return 0;
			else
				return 1;
		else
			return 1;
	}
	
	public RPGObject draw(SpriteBatch batch,float parentAlpha){
		if(isVisible()){
			if(this.drawShadow){
				shadow.position(getX()+8f, getY()-3.8f).color(this.getColor()).draw(batch,parentAlpha);
			}
			this.getCurrentImage().setColor(this.getColor());
			this.getCurrentImage().draw(batch,parentAlpha);
			if(displayBalloon){
				bon.getCurrentImage().setPosition(getX()+Balloon.ANIMATION_SIZE, getY()+getHeight()+Balloon.ANIMATION_SIZE/2);
				bon.getCurrentImage().setColor(this.getColor());
				bon.getCurrentImage().draw(batch,parentAlpha);
			}
		}
		return this;
	}
	
	public RPGObject(String path,int width,int height){
		imgPath=path;
		bodyWidth=width;
		bodyHeight=height;
		images=RPGObject.generateImages(path, width, height);
	}
	
	public RPGObject setBalloon(BalloonType type){
		this.bon.setBalloons(new Balloon(type).getBalloons()).reset();
		return this.setDisplayBalloon(true);
	}
	
	public RPGObject resetBalloon(BalloonType type){
		this.bon.reset();
		return this.setDisplayBalloon(true);
	}
	
	public RPGObject setDisplayBalloon(boolean flag){
		this.displayBalloon=flag;
		return this;
	}
	
	public static Image[] generateImages(String txt,int width,int height){
		Image[] images=new Image[12];
		for(int i=0;i<4;i++)
			for(int j=0;j<3;j++){
				images[(i*3)+j]=new Image(new TextureRegion(Res.getTexture(txt),j*width,i*height,width,height));
			}
		return images;
	};
	
	public RPGObject turn(int face){
			this.currentImageNo=face;
		return this;
	}
	
	public float walkSpeed=3f;
	
	public Vector2 lastPosition;
	public RPGObject walk(int step){
		walkStack.add(new MoveStack(this.getCurrentFace(),step));
		return this;
	}
	
	private int getCurrentFaceByRev(){
		if(calcRevStep()==1)
			return getCurrentFace();
		else
			return getReverseFace();
	}
	
	public int getCurrentFace(){
		if(this.currentImageNo==0 || this.currentImageNo==1 || this.currentImageNo==2)
			return FACE_D;
		if(this.currentImageNo==3 || this.currentImageNo==4 || this.currentImageNo==5)
			return FACE_U;
		if(this.currentImageNo==6 || this.currentImageNo==7 || this.currentImageNo==8)
			return FACE_L;
		else
			return FACE_R;
	}
	
	private int NEXT_FOOT=0;
	private int lastx,lasty,lastlength;
	private int fixTime=0;
	@Override
	public void act(float f) {
		if (isStop() && lastlength++ > 5)
			foot = 0;
		//帧数补偿
		//当游戏帧数小于60FPS时，将尝试加快IRPGObject的行走速度而达到补偿的目的
		int fps= Gdx.graphics.getFramesPerSecond();
		fixTime += 60-fps;
		if (fixTime < 0){
			fixTime = 0;
		}else if(fixTime >= 60){
			int fixCount= (int) (fixTime/60f);
			for(int i=0;i<fixCount;i++){
				toWalk();
			}
			fixTime-=fixCount*60;
		}

		toWalk();
		
		if(displayBalloon && !bon.isStop())
			bon.act(fps);
		else
			displayBalloon=false;
	}
	
	public boolean isStop(){
		boolean flag=false;
		flag=((lastx==(int)position.x) && (lasty==(int)position.y));
		lastx=(int) position.x;
		lasty=(int) position.y;
		return flag;
	}
	/**
	 * 下面这段代码是2015年7月30日20:29:42写的，我发誓3个月后来看一定会自己揍自己一拳：）
	 * @return
	 */
	private int calcRevStep(){
		return walkStack.isEmpty() || walkStack.get(0).step == 0 ? lastWalkSize > 0 ? 1 : -1 : walkStack.get(0).step > 0 ? 1 : -1;
	}
	
	public boolean toWalk(){
		if(!walked){
			lastlength=0;
			switch(getCurrentFace()){
			case FACE_D:{
				if (Math.abs(lastPosition.y-position.y)==48){
					if(testWalk()==null)
						return false;
				}else
					if(Math.abs(lastPosition.y-position.y)+walkSpeed<=48)
						position.y-=walkSpeed*calcRevStep();
					else{
						position.y-=(48-Math.abs(lastPosition.y-position.y))*calcRevStep();
						return true;
					}
				break;
			}
			case FACE_U:{
				if (Math.abs(lastPosition.y-position.y)==48){
					if(testWalk()==null)
						return false;
				}else
					if(Math.abs(lastPosition.y-position.y)+walkSpeed<=48)
						position.y+=walkSpeed*calcRevStep();
					else{
						position.y+=(48-Math.abs(lastPosition.y-position.y))*calcRevStep();
						return true;
					}
				break;
			}
			case FACE_L:{
				if (Math.abs(lastPosition.x-position.x)==48){
					if(testWalk()==null)
						return false;
				}else
					if(Math.abs(lastPosition.x-position.x)+walkSpeed<=48)
						position.x-=walkSpeed*calcRevStep();
					else{
						position.x-=(48-Math.abs(lastPosition.x-position.x))*calcRevStep();
						return true;
					}
				break;
			}
			case FACE_R:{
				if (Math.abs(lastPosition.x-position.x)==48){
					if(testWalk()==null)
						return false;
				}else
					if(Math.abs(lastPosition.x-position.x)+walkSpeed<=48)
						position.x+=walkSpeed*calcRevStep();
					else{
						position.x+=(48-Math.abs(lastPosition.x-position.x))*calcRevStep();
						return true;
					}
				break;
			}
			}
			if((NEXT_FOOT-=walkSpeed)<=0){
				if(++foot==3)
					foot=-1;
				NEXT_FOOT=(int) (walkSpeed*12);
			}
		}
		return false;
	}
	
	public Boolean testWalk(){
		if(walkStack.size()!=0){
			if(enableCollide && ((getCurrentFaceByRev()==FACE_L && !collide.left) || (getCurrentFaceByRev()==FACE_R && !collide.right) 
			|| (getCurrentFaceByRev()==FACE_U && !collide.top) || (getCurrentFaceByRev()==FACE_D && !collide.bottom))){
				walkStack.get(0).step=0;
				if(!waitWhenCollide){
					testWalkerSize();
				}else
					return null;
			}else{
				if(walkStack.get(0).step!=0){
					lastZ=this.layer;
					lastFace=this.getCurrentFace();
					lastWalkSize=walkStack.get(0).step;
					
					switch(getCurrentFace()){
					case FACE_D:{mapy+=calcRevStep();break;}
					case FACE_U:{mapy-=calcRevStep();break;}
					case FACE_L:{mapx-=calcRevStep();break;}
					case FACE_R:{mapx+=calcRevStep();break;}
					}
					walkStack.get(0).step+=walkStack.get(0).step>=0?-1:1;
					lastPosition=new Vector2(position.x, position.y);
					walked=false;
					toWalk();
					return true;
				}else{
					testWalkerSize();
				}
			}
		}else{
			walked=true;
		}
		return false;
	}
	
	private RPGObject testWalkerSize(){
		walkStack.remove(walkStack.get(0));
		return this;
	}
	
	public RPGObject setWalkSpeed(float s){
		this.walkSpeed=s;
		return this;
	}
	
	public RPGObject resetWalkSeped(){
		this.walkSpeed=NORMAL_WALK_SPEED;
		return this;
	}
	
	public float getWalkSpeed(){
		return walkSpeed;
	}
	
	public int getCurrentFoot(){
		return getCurrentFace()+(foot==2?0:foot);
	}
	
	@Override
	public float getX(){
		return this.position.x;
	}
	
	@Override
	public float getY(){
		return this.position.y;
	}
	
	@Override
	public float getWidth(){
		return this.getCurrentImage().getWidth();
	}
	
	@Override
	public float getHeight(){
		return this.getCurrentImage().getHeight();
	}
	
	public RPGObject generatePosition(int mapx,int mapy,int layer){
		
		int yoff = (int) Math.ceil(getHeight()/48);
		
		try {
			TiledMapTileLayer l=(TiledMapTileLayer)RPG.maps.loader.layer.get(layer);
			this.mapx=mapx;
			this.mapy= mapy-yoff;
			this.layer=layer;
			this.position=new Vector2(mapx*48,(l.getHeight()-this.mapy-1)*48);
			lastPosition=new Vector2(this.position);
		} catch (Exception e) {
			Logger.error("无法生成RPG对象位置！请检查坐标设置是否正确>>["+e.getMessage()+"]");
			e.printStackTrace();
		}
		return this;
	}
	
	public RPGObject generateAbsolutePosition(int x,int y,int layer){
		try {
			this.mapx=x/48;
			this.mapy= y/48;
			this.layer=layer;
			this.position=new Vector2(x,y);
			lastPosition=new Vector2(this.position);
		} catch (Exception e) {
			Logger.error("无法生成RPG对象位置！", e);
		}
		return this;
	}
	
	
	
	public static int getReverseFace(int face){
		if(face==FACE_L)
			return FACE_R;
		if(face==FACE_D)
			return FACE_U;
		if(face==FACE_R)
			return FACE_L;
		else
			return FACE_D;
	}
	
	public int getReverseFace(){
		return getReverseFace(getCurrentFace());
	}
	
	public int getFaceByPoint(int x,int y){
		boolean xable=MathUtils.random(100)>50;
		if(xable)
			return mapx>x?FACE_L:FACE_R;
		else
			return mapy>y?FACE_U:FACE_D;
	}
	
}
