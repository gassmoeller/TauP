/*
  The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
  Copyright (C) 1998-2000 University of South Carolina

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

  The current version can be found at 
  <A HREF="www.seis.sc.edu">http://www.seis.sc.edu</A>

  Bug reports and comments should be directed to 
  H. Philip Crotwell, crotwell@seis.sc.edu or
  Tom Owens, owens@seis.sc.edu

*/

package edu.sc.seis.TauP;

import java.util.Vector;
import java.io.*;

/**
  * This class provides storage and methods for generating slowness-depth
  * pairs in a spherical earth model.
  *
  * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001



  * @author H. Philip Crotwell
  *
  */
public class SphericalSModel extends SlownessModel
      implements Serializable, Cloneable {

// METHODS ----------------------------------------------------------------

      /** Just for debugging purposes. */
   public static void main(String[] args) {
	   System.out.println("Starting main");
		
      VelocityModel vMod = new VelocityModel();
      SphericalSModel sMod = new SphericalSModel();
      String modelFilename;
      if (args.length == 1) {
         modelFilename = args[0];
      } else {
      	vMod.setFileType("tvel");
         modelFilename = File.separator+"MacintoshHD"+File.separator+"Philip"+File.separator+"TauP"+File.separator+"VModels"+File.separator+"iasp91.tvel";
      }
      boolean DEBUG = true;

      try {
         DEBUG = true;
         vMod.readVelocityFile(modelFilename);
         System.out.println("Done reading.");
         if (DEBUG) System.out.println(vMod);
         DEBUG = true;
 
         sMod.DEBUG = true;

         sMod.createSample(vMod);

         if (sMod.DEBUG) System.out.println(sMod);
         sMod.validate();
      } catch (IOException e) {
         System.out.println("Tried to read!\n Caught IOException "
                            + e.getMessage()+"\n"+e.getClass().getName());
					e.printStackTrace();
      } catch (VelocityModelException e) {
         System.out.println("Tried to read!\n Caught VelocityModelException "
                            + e.getMessage());
					e.printStackTrace();
      } catch (SlownessModelException e) {
         System.out.println(vMod);
         System.out.println(sMod);
         System.out.println("Tried to create slowness!\n Caught SlownessModelException "
                            + e.getMessage());
					e.printStackTrace();
      } finally {
         System.out.println("Done!\n");
      }

   }
   
      /** Returns the slowness for a velocity at a depth. 
		 *  @exception SlownessModelException if velocity is zero. */
   public double toSlowness(double velocity, double depth) throws SlownessModelException {
		if (velocity == 0.0) {
			throw new SlownessModelException("Divide by zero in toSlowness()"+
				"\ndepth = "+depth+"\nThis likely has to do with using S velocities in the outer core");
		}
      return (radiusOfEarth-depth)/velocity;
   }
   
      /** Returns the velocity for a slowness at a depth. 
		 *  @exception SlownessModelException if slowness is zero. */
   public double toVelocity(double slowness, double depth) throws SlownessModelException {
		if (slowness == 0.0) {
			throw new SlownessModelException("Divide by zero in toVelocity()"+
				"\ndepth = "+depth+"\nPossibly this is due to depth at center of the earth?");
		}
      return (radiusOfEarth-depth)/slowness;
   }

      /** Converts a velocity layer into a slowness layer. 
       *  @exception SlownessModelException if velocity layer is malformed. */
   public SlownessLayer toSlownessLayer(VelocityLayer vLayer, boolean isPWave) 
	throws SlownessModelException {
       return new SlownessLayer(vLayer, true, radiusOfEarth, isPWave);
   }

      /** Returns the depth for a slowness given a velocity gradient. 
		 *  @exception SlownessModelException if the velocity gradient
		 *     exactly balances the spherical decrease in slowness. */
   public double interpolate(double p, double topVelocity, double topDepth,
         double slope) throws SlownessModelException {
      double depth;
      double denominator = p * slope + 1.0;

      if (denominator != 0.0) {
         depth = (radiusOfEarth +
            p* (topDepth*slope-topVelocity))/ denominator;
      } else {
            /* Uh oh, this is a neg velocity gradient that just
             * balances the slowness gradient of the spherical
             * slowness. In this case we should equally space the
             * depths. ????
             * This probably won't happen, but...
             */ 
         depth = Double.MAX_VALUE;
         throw new SlownessModelException("Neg velocity gradient "+
            "just balances the earth flattening!"+
            " What should I do?!?!?!? topDepth= "+topDepth);
      }
      return depth;
   }

      /** Calculates the time and distance increments accumulated by a
       *  ray of spherical ray parameter p when passing through layer layerNum.
       *  for the easy cases of zero ray parameter, the center of the earth,
       *  and constant velocity layers.
       *  Note that this gives 1/2 of the true range and time increments since
       *  there will be both an up going and a downgoing path.
       *
       *  @exception SlownessModelException occurs if the ray with the given
       *     spherical ray parameter cannot propagate within this layer, or
       *     if the ray turns within this layer but not at the bottom.
       */
   public TimeDist layerTimeDist(double sphericalRayParam, int layerNum, 
   boolean isPWave) throws SlownessModelException {

      double swapDouble;
      double b;        // temporary variable makes the calculations less ugly.
 
         // To hold the return values.
      TimeDist timedist = new TimeDist(sphericalRayParam);

      SlownessLayer sphericalLayer = getSlownessLayer(layerNum, isPWave);
      double topRadius = radiusOfEarth-sphericalLayer.topDepth; // radius to top
      double botRadius = radiusOfEarth-sphericalLayer.botDepth; // radius to bot

         /* First we make sure that a ray with this ray parameter can propagate
          * within this layer and doesn't turn in the middle of the layer.
          * If not, then throw an exception. */
      if (sphericalRayParam > 
          Math.max(sphericalLayer.topP,sphericalLayer.botP)) {
         throw new SlownessModelException("Ray cannot propagate within this"+
            " layer. layerNum = "+ layerNum+
            " sphericalRayParam="+sphericalRayParam+"\n"+sphericalLayer);
      }
      if (sphericalRayParam < 0.0) {
         throw new SlownessModelException("Ray Parameter is negative!!! "+
            sphericalRayParam);
      } 
      if (sphericalRayParam > 
            Math.min(sphericalLayer.topP,sphericalLayer.botP)) {
         if (DEBUG) {
            System.out.println("Ray Turns in layer, velocities: "+
               topRadius/sphericalRayParam+" "+
               topRadius/sphericalLayer.topP+" "+
               botRadius/sphericalLayer.botP);
            System.out.println("depths        top "+sphericalLayer.topDepth+
               "  bot "+sphericalLayer.botDepth);
         }
         throw new SlownessModelException("Ray turns in the middle of this"+
            " layer. \nlayerNum = "+ layerNum+
            " sphericalRayParam "+sphericalRayParam+
            " sphericalLayer =  "+sphericalLayer+"\n");
      }

         /* Check to see if this layer has zero thickness, if so then it is
          * from a critically reflected slowness sample. So we should just
          * return 0.0 for time and distance increments. */
      if (sphericalLayer.topDepth == sphericalLayer.botDepth) {
         timedist.time = 0.0;
         timedist.dist = 0.0;
         return timedist;
      }
 
         /* Check to see if this layer contains the center of the earth. 
          * If so then
          * the spherical ray parameter should be 0.0 and we calculate 
          * the range and
          * time increments using a constant velocity layer (sphere). 
          * See eq 43 and 44
          * of Buland and Chapman, although we implement them slightly 
          * differently. 
          * Note that the distance and time increments are 
          * for just downgoing or just up going, ie top of the layer 
          * to the center of
          * the earth or vice versa but not both. This is in keeping with 
          * the convention
          * that these are one way distance and time increments. We will
          * multiply the result by 2 at the end, or if we are doing a 
          * 1.5D model, the
          * other direction may be different.
          * The time increment for a ray of zero ray parameter passing
          * half way through a sphere of constant velocity is just the spherical
          * slowness at the top of the sphere. An amazingly simple result!
          */
      if (sphericalRayParam == 0.0 && sphericalLayer.botDepth == radiusOfEarth){
         if (layerNum != getNumLayers(isPWave)-1) throw new SlownessModelException(
             "There are layers deeper than the center of the earth!");
             
         timedist.dist = Math.PI/2.0;
         timedist.time = sphericalLayer.topP;
        
         if (DEBUG) {
            System.out.println("Center of Earth: dist "+timedist.dist+
               " time "+timedist.time);
         }
         if (timedist.dist<0.0 || timedist.time<0.0 ||
             Double.isNaN(timedist.time) || Double.isNaN(timedist.dist)) {
            throw new SlownessModelException("CoE timedist <0.0 or NaN: "+
               "sphericalRayParam= "+sphericalRayParam+
               " botDepth = "+sphericalLayer.botDepth+
               " dist="+timedist.dist+
               " time="+timedist.time);
         }
         return timedist;
      }
       
         /* Now we check to see if this is a constant velocity layer and
          * if so than we can do a simple triangle calculation to get 
          * the range and time increments. 
          * To get the time increment we first calculate the path length
          * through the layer using law of cosines, noting that the angle
          * at the top of the layer can be obtained from the spherical Snell's
          * Law. The time increment is just the path length divided by 
          * the velocity. To get the distance we first find the angular 
          * distance traveled, using law of sines.
          */
      if (Math.abs(topRadius/sphericalLayer.topP -
                   botRadius/sphericalLayer.botP) < slownessTolerance ) {

            // temp variables
         double vel = botRadius / sphericalLayer.botP;              // velocity
            /* In cases of a ray turning at the bottom of the layer numerical
             * roundoff can cause botTerm to be very small (1e-9) but negative
             * which causes the sqrt to return NaN. We check for values that
             * are within the numerical chatter of zero and just set them to
             * zero. */
         double topTerm, botTerm;
         topTerm = topRadius*topRadius-
            sphericalRayParam*sphericalRayParam*vel*vel;
         if (Math.abs(topTerm) < slownessTolerance) {topTerm = 0.0;}

         if (sphericalRayParam == sphericalLayer.botP) {
               /* In this case the ray turns at the bottom of this layer
                * so sphericalRayParam*vel == botRadius and botTerm should
                * be zero. We check for this case specially because numerical
                * chatter can cause small round offs that lead to botTerm being
                * negative, causing a sqrt(-1) error.  */
            botTerm = 0.0;
         } else {
            botTerm = botRadius*botRadius-
               sphericalRayParam*sphericalRayParam*vel*vel;
         }
         
            // Use b for temp storage of the length of the ray path.
         b = Math.sqrt(topTerm) - Math.sqrt(botTerm);

         timedist.time = b/vel;

         timedist.dist=Math.asin(b*sphericalRayParam*vel/(topRadius*botRadius));

         if (timedist.dist<0.0 || timedist.time<0.0 ||
             Double.isNaN(timedist.time) || Double.isNaN(timedist.dist)) {
            throw new SlownessModelException("CVL timedist <0.0 or NaN: "+
               "\nsphericalRayParam= "+sphericalRayParam+
               "\n botDepth = "+sphericalLayer.botDepth+
               "\n topDepth = "+sphericalLayer.topDepth+
               "\n topRadius="+topRadius+" botRadius="+botRadius+
               "\n dist="+timedist.dist+
               "\n time="+timedist.time+
               "\n b="+b+
               "\n topTerm="+topTerm+
               "\n botTerm="+botTerm+
               "\n vel    ="+vel+"\n"+
               "\n bR^2   ="+(botRadius*botRadius)+
               "\n p^2v^2 ="+sphericalRayParam*sphericalRayParam*vel*vel+
               "\n tR^2   ="+(topRadius*topRadius)+
               "\n p^2v^2 ="+sphericalRayParam*sphericalRayParam*vel*vel);

         }
         return timedist;
      }

         /* OK, the layer is not a constant velocity layer or 
          * the center of the earth and p is not zero
          * so we have to do it the hard way...
          *
          */
      return sphericalLayer.bullenRadialSlowness(sphericalRayParam, 
         radiusOfEarth);
   }
    
      /** Performs consistency check on the velocity model.
       *  @return true if successful, throws SlownessModelException otherwise.
       *  @exception SlownessModelException if any check fails
       */
   public boolean validate() 
      throws SlownessModelException 
   {
      boolean isOK = super.validate();
   
      double prevDepth= 0.0;
      DepthRange highSZoneDepth, fluidZone;
      SlownessLayer sLayer;

      boolean isPWave = true;
      for (int j=0;j<2; j++, isPWave = false) {
         for (int i=0;i<getNumLayers(isPWave);i++) {
            sLayer = getSlownessLayer(i,isPWave);
            prevDepth = sLayer.botDepth;
         
         /* No slowness layer should have a depth greater than radiusOfEarth.*/
            if (prevDepth > radiusOfEarth) {
               isOK = false;
               throw new SlownessModelException(
                  "Slowness layer has a depth larger than the radius of "+
                  "the earth in a spherical model. max depth = "+prevDepth+
                  " radiusOfEarth = "+radiusOfEarth);
            } else {
               isOK |= true;
            }
         }
      }
      

         /* Everything checks out OK so return true. */
      return isOK;
   }

      /* Returns a clone of this slowness model. All fields are correctly
       * copied so modifications to the clone do not affect the original. */
   public Object clone() {
      SphericalSModel newObject = (SphericalSModel)super.clone();

      return newObject;
   }

   public String toString() {
      int topCriticalLayerNum;
      int botCriticalLayerNum;

      String desc = "spherical model:\n"+super.toString();

      return desc;
   }

}
