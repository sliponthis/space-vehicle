import org.orekit.data.*;
import org.orekit.errors.*;
import org.orekit.orbits.OrbitType;
import java.io.*;

public class TLEGenerator
{
  private double a = 20.2e6;                  // semi major axis in m
  private double e = 0.05;                    // eccentricity
  private double i = Math.toRadians(30);      // inclination
  private double w = Math.toRadians(0);       // perigee argument
  private double omega = Math.toRadians(0);   // right ascension of ascending node
  private double lM = 0;                      // mean anomaly
  private File orekitData;
  private DataProvidersManager manager;

  public static void main(String[] args)
  {
    OrbitType orbitType = OrbitType.CARTESIAN;
    TLEGenerator gen = new TLEGenerator();
    Satellite sat = new Satellite(gen.w, gen.omega, gen.lM, gen.i, gen.a, gen.e);
    sat.setPropagator();
        /* String TLEdata = sat.sPropagate("Two Body");
        writeTLEToDisk(TLEdata,gen.TLEdata_twoBody);
        TLEdata = sat.sPropagate("Ekstein");
        writeTLEToDisk(TLEdata,gen.TLEdata_J6);*/

    //sat.mPropagate(incorporateBurn);
    //writeTLEToDisk(Satellite.StepHandler.dataContainer, gen.TLEdata_J10);

  }

  public TLEGenerator()
  {
    orekitData = new File("/Users/ryanvary/IdeaProjects/orekit-data");
    manager = DataProvidersManager.getInstance();
    try
    {
      manager.addProvider(new DirectoryCrawler((orekitData)));
    }
    catch(OrekitException oke)
    {
      System.out.println(oke.getMessage());
    }
  }

  public static void writeTLEToDisk(String data, String relativePath)
  {
    BufferedWriter bw;
    String absolutePath = "/Users/ryanvary/IdeaProjects/SpaceVehicle/out/results/";
    File f = new File(absolutePath + relativePath);
    try
    {
      bw = new BufferedWriter(new FileWriter(f));
      bw.write(data);
      bw.close();
    }
    catch(IOException ioException)
    {
      System.out.println(ioException.getMessage());
    }
  }
}