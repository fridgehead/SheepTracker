import java.awt.Point;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import hypermedia.video.Blob;
import javax.media.jai.*;

import org.yaml.snakeyaml.Yaml;

import processing.core.PImage;



public class FieldModel {
	private SheepTest parent;
	private ArrayList<Point2D> sheepList = new ArrayList<Point2D>();
	public ArrayList<Tank> tankList = new ArrayList<Tank>();
	PerspectiveTransform sheepTransform, tankTransform;
	PImage sheepImage, tankImage;
	private int compassCorrection = 0;
	public Point flockCenter = new Point(0,0);
	private Yaml yaml;
	Point basePos;
	
	public TankController tankController;


	public FieldModel(SheepTest parent){
		this.parent = parent;
		sheepTransform = new PerspectiveTransform();
		Point.Float[] p = new Point.Float[4];
		p[0] = new Point.Float(0,0);
		p[1] = new Point.Float(640,0);
		p[2] = new Point.Float(640,480);
		p[3] = new Point.Float(0,480);
		setSheepTransform(new CalibrationQuad(p));
		setTankTransform(new CalibrationQuad(p));
		sheepImage = parent.loadImage("sheep.png");
		tankImage = parent.loadImage("tank.png");
		yaml = new Yaml();
		loadSettings();
		
		tankController = new TankController(parent);
		tankController.start();
		
	}

	public void loadSettings(){
		try{
			Point.Float[] gpsPoints = new Point.Float[4];
			InputStream input = new FileInputStream("settings.yaml");

			for(Object obj : yaml.loadAll(input)){				
				Map<String, Object> objMap = (Map<String, Object>)obj;
				
				ArrayList quadList = (ArrayList)objMap.get("gpsQuad");

				for(Object quad: quadList){
					Map<String, Object> qPoint = (Map<String, Object>)quad;
					int id =  ((Integer)qPoint.get("id")).intValue();
					float xp = ((Double)qPoint.get("x")).floatValue();
					float yp = ((Double)qPoint.get("y")).floatValue();
					gpsPoints[id] = new Point.Float(xp,yp);
					System.out.println("loaded gps coord: " + id + " - " + gpsPoints[id]);
					
					
				}
			}
			
			setTankTransform(gpsPoints);
		} catch (Exception e){
			e.printStackTrace();
		}
		
		
	}

	public void setCompassCorrection(int in){
		compassCorrection = in;
	}

	/*
	 * Take a tank update, using the gpstransform translate coords back to fieldspace from gps lat/lon
	 */
	public void updateTank(TankServer.TankUpdate t){
		boolean updated = false;
		for(Tank tank : tankList){
			if(t.tankId == tank.tankId){
				if(t.type == TankServer.TankUpdate.COMPASS){
					tank.heading = (t.heading + compassCorrection) % 360;

				} else if (t.type == TankServer.TankUpdate.POSITION){
					tank.lat = t.lat;
					tank.lon = t.lon;
					tank.setPositionFromGPS(t.lat, t.lon);
					Point2D p = null;
					p = tankTransform.transform((Point2D)tank.worldPosition, p);

					tank.fieldPosition = new Point((int)p.getX(), (int)p.getY());
					//tank.fieldPosition = new Point(200,200);
					//System.out.println(tank);

				} else if (t.type == TankServer.TankUpdate.QUIT){
					//remove this tank
					tankList.remove(tank);
					System.out.println("Removed tank: " + t.tankId);
				}
				updated = true;
			}
		}

		if(!updated){
			Tank tank = new Tank(t.tankId, new Point(200 + (int)(Math.random() * 30),200));
			tankList.add(tank);
			System.out.println("new tank");
		}

	}


	/*
	 * takes a list of sheep blobs, transforms them to field-space and stores them
	 */
	public void updateSheepPositions(ArrayList<Point> sheepListIn){
		//take each entry and run it through the transformation to field-space
		sheepList.clear();
		int flockCount = 0;
		flockCenter = new Point(0,0);


		for (Point b : sheepListIn){
			Point2D p = null;

			p = sheepTransform.transform(b, p);

			sheepList.add(p);
			flockCenter.x += p.getX();
			flockCenter.y += p.getY();

			flockCount ++;


		}
		if(flockCount > 0){
			flockCenter.x = flockCenter.x / flockCount;
			flockCenter.y = flockCenter.y / flockCount;
		}
	}

	public void setSheepTransform(Point.Float[] p){
		setSheepTransform(new CalibrationQuad(p));
	}

	/*
	 * Maps all sheep coordinates from camera space to field space
	 */
	public void setSheepTransform(CalibrationQuad quad){
		sheepTransform = new PerspectiveTransform();
		sheepTransform = PerspectiveTransform.getQuadToQuad(quad.p1.x, quad.p1.y, 
				quad.p2.x, quad.p2.y, 
				quad.p3.x, quad.p3.y,
				quad.p4.x, quad.p4.y, 
				0.0f, 0.0f, 
				800.0f, 0.0f, 
				800.0f, 600.0f, 
				0.0f, 600.0f);


		System.out.println("SheepTransform: " + sheepTransform);
	}

	public void setTankTransform(Point.Float[] p){
		setTankTransform(new CalibrationQuad(p));
	}

	/*
	 * Maps all sheep coordinates from camera space to field space
	 */
	public void setTankTransform(CalibrationQuad quad){
		tankTransform = new PerspectiveTransform();
		tankTransform = PerspectiveTransform.getQuadToQuad((float)quad.p1.x, (float)quad.p1.y, 
				(float)quad.p2.x, (float)quad.p2.y, 
				(float)quad.p3.x, (float)quad.p3.y,
				(float)quad.p4.x, (float)quad.p4.y, 
				0.0f, 0.0f, 
				800.0f, 0.0f, 
				800.0f, 600.0f, 
				0.0f, 600.0f);


		System.out.println("Tank Transform: " + tankTransform);
	}


	public ArrayList<Point2D> getSheepList(){
		return sheepList;
	}

	public void draw(Point basePos){
		this.basePos = basePos;
		parent.pushMatrix();
		parent.translate(basePos.x, basePos.y);
		parent.pushStyle();
		parent.stroke(255,255,255);
		parent.fill(0,150,0);
		parent.rect(0,0,800,600);
		parent.fill(255,255,255);
		for(Point2D p : sheepList){

			//parent.ellipse((float)p.getX(),(float)p.getY(),10,10);
			parent.image(sheepImage, (float)p.getX() - 20, (float)p.getY() - 16);
		}

		for(Tank t : tankList){
			parent.pushMatrix();
			parent.translate((float)t.fieldPosition.getX() ,(float)t.fieldPosition.getY());
			parent.rotate(parent.radians(t.heading));
			parent.translate(-16,-16);
			
			parent.image(tankImage, 0,0 );
			if(t.selected){
				parent.stroke(255,0,0);
				parent.noFill();
				parent.rect(0, 0, 32, 32);
				
			}
			parent.popMatrix();
			parent.textFont(parent.myFont,10);
			parent.text(t.tankId, (float)t.fieldPosition.getX(),(float)t.fieldPosition.getY());

		}

		parent.fill(0,255,0);
		parent.ellipse(flockCenter.x, flockCenter.y, 10,10);

		parent.popStyle();
		parent.popMatrix();


	}

	public class CalibrationQuad{
		Point.Float p1,p2,p3,p4;
		public CalibrationQuad(Point.Float[] p){
			p1 = p[0];
			p2 = p[1];
			p3 = p[2];
			p4 = p[3];

		}

		public String toString(){
			return "p1: " + p1 + " p2: " + p2 + "\np3: " + p3 + " p4: " + p4;
		}
	}

	public void mouseClicked(int mouseX, int mouseY) {
		for(Tank tank : tankList){
			tank.selected = false;
			if(mouseX - basePos.x > tank.fieldPosition.x -16  && mouseX - basePos.x < tank.fieldPosition.x + 16){
				if(mouseY - basePos.y > tank.fieldPosition.y -16 && mouseY - basePos.y < tank.fieldPosition.y + 16){
					tank.selected = true;
				}
			}
		}
	}

	public void keyPressed(int keyCode) {
		for(Tank t : tankList){
			if(t.selected){
				switch (keyCode){
					case 38:	//up
						tankController.go(t.tankId, 1);
						break;
					
					case 39:	//right
						tankController.rotate(t.tankId, 1);
						break;
					case 40:	//down
						tankController.go(t.tankId, -1);
						break;
					case 37:	//left
						tankController.rotate(t.tankId, -1);
						break;
					case 32:		//space
						tankController.stopMoving(t.tankId);
						tankController.stopRotate(t.tankId);
						break;
				}
			}
		}
	}

}
