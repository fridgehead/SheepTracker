import java.awt.Color;
import java.awt.Point;


public class TankPoint {
	/**
	 * 
	 */
	private final TankIdentifier tankIdentifier;
	public Color colour;
	public Point position;
	public int pointId = 0;
	public String colourName = "";
	public int colourId = -1;

	private String[] names = new String[9];
	private int[] colours = new int[9];


	public TankPoint(TankIdentifier tankIdentifier){
		this.tankIdentifier = tankIdentifier;
		colours[0] = 0xFF0000; 
		names[0] =  "Red";

		colours[1] = 0xFFA500; 
		names[1] = "Orange";
		colours[2] = 0xFFFF00; 
		names[2] = "Yellow";
		colours[3] = 0x008000; 
		names[3] = "Green";
		colours[4] = 0x0000FF; 
		names[4] = "Blue";
		colours[5] = 0xEE82EE; 
		names[5] = "Violet";
		//colours[6] = 0xA52A2A; 
		//names[6] = "Brown";
		colours[6] = 0x000000; 
		names[6] = "Black";
		colours[7] = 0x808080; 
		names[7] = "Grey";
		colours[8] = 0xFFFFFF; 
		names[8] = "White";
	}

	public void setColour(Color c){
		colour = c;

		  int r = c.getRed();
		  int g = c.getGreen();
		  int b = c.getBlue();

		  HSLColor hsl = new HSLColor(c);
		  int h = (int)(hsl.getHue() * 0.708333333333333);
		  int s = (int)(hsl.getSaturation() * 2.55);
		  int l = (int)(hsl.getLuminance() * 2.55);
		  float ndf1 = 0; 
		  float ndf2 = 0; 
		  float ndf = 0;
		  int cl = -1;
		  float df = -1;

		  for(int i = 0; i < names.length; i++)
		  {
		    
		    ndf1 = (float)(Math.pow(r - this.tankIdentifier.parent.red(colours[i]), 2) + Math.pow(g - this.tankIdentifier.parent.green(colours[i]), 2) + Math.pow(b - this.tankIdentifier.parent.blue(colours[i]), 2));
		    ndf2 = (float)(Math.abs(Math.pow(h - this.tankIdentifier.parent.hue(colours[i]), 2)) + Math.pow(s - this.tankIdentifier.parent.saturation(colours[i]), 2) + Math.abs(Math.pow(l - this.tankIdentifier.parent.brightness(colours[i]), 2)));
		    ndf = ndf1 + ndf2 * 2;
		    if(df < 0 || df > ndf)
		    {
		      df = ndf;
		      cl = i;
		    }
		  }

		  if(cl != -1){
		    colourName =  names[cl];
		    colourId = cl;
		  } else {
		    colourName = "";
		  }
		  

	}


}