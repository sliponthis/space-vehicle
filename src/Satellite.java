import org.hipparchus.geometry.Space;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.*;
import org.orekit.forces.gravity.potential.*;
import org.orekit.forces.gravity.*;
import org.orekit.forces.maneuvers.*;
import org.orekit.forces.*;
import org.orekit.propagation.*;
import org.orekit.propagation.analytical.*;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.*;
import org.orekit.propagation.sampling.*;
import org.orekit.time.*;
import org.orekit.errors.*;
import org.hipparchus.ode.nonstiff.*;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

public class Satellite
{
  static double mu = 3.986e14;
  static double re = 6.371e6;
  private static String dataContainer;
  private double w, omega, nu, i, a, e, _period, del_t;
  private Orbit orbit;
  private AbsoluteDate initialDate, finalDate;
  private FactoryManagedFrame inertialFrame;
  private SpacecraftState vehicleState;
  private Collection<EventDetector> events;

  public Satellite(double perigee, double ascension, double trueAnomaly, double inclination,
                   double semiMajorAxis, double eccentricity)
  {
    w = perigee;
    omega = ascension;
    i = inclination;
    a = semiMajorAxis;
    e = eccentricity;
    nu = trueAnomaly;
    inertialFrame = FramesFactory.getEME2000();
    try
    {
      UTCScale _utc = TimeScalesFactory.getUTC();
      initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, _utc);
      orbit = new KeplerianOrbit(a, e, i, w, omega, nu, PositionAngle.TRUE, inertialFrame, initialDate, mu);
      orbit = new CartesianOrbit(orbit);
      vehicleState = new SpacecraftState(orbit);
      del_t = computeOrbitalProperties(200);
    }
    catch (OrekitException ex)
    {
      System.out.println(ex.getMessage());
    }
  }

  private double computeOrbitalProperties(int steps)
  {
    double n, del_t;
    n = Math.sqrt(mu / Math.pow(a, 3));
    _period = (2 * Math.PI) / n;
    finalDate = initialDate.shiftedBy(_period);
    del_t = _period / steps;
    return del_t;
  }

  private AbstractPropagator getPropagator(String type)
  {
    String model1 = "Two Body", model2 = "Eckstein", model3 = "J6";
    double c20 = -1.08e-3;  //un-normalized zonal coeffcients
    double c30 = 2.53e-6;
    double c40 = 1.62e-6;
    double c50 = 2.28e-7;
    double c60 = -5.41e-7;
    AbstractPropagator analyticalProp;
    try
    {
      if (model1.equals(type)) analyticalProp = new KeplerianPropagator(orbit);
      else analyticalProp = new EcksteinHechlerPropagator(orbit, re, mu, c20, c30, c40, c50, c60);
      analyticalProp.setSlaveMode();
      return analyticalProp;
    }
    catch (OrekitException oke)
    {
      System.out.println(oke.getMessage());
      return null;
    }
  }

  protected void addEventHandlers(AbstractPropagator propagator)
  {
    AbsoluteDate date = initialDate.shiftedBy(del_t*9.2);
    DateDetector detector = new DateDetector(initialDate.shiftedBy(del_t*100.8))
            .withMaxCheck(del_t*0.001)
            .withMaxIter(500)
            .withThreshold(.39756)
            .withHandler(new DateHandler());
    propagator.addEventDetector(detector);
  }

  protected void setPropagator()
  {
    double minStep = 0.0001, maxStep = 1000.0, positionTolerance = 10.0;
    AdaptiveStepsizeIntegrator integrator;
    NumericalPropagator numericalProp;
    NormalizedSphericalHarmonicsProvider provider;
    ForceModel holmesFeatherstone;
    try
    {
      double[][] tolerances = NumericalPropagator.tolerances(positionTolerance, orbit, orbit.getType());

      integrator = new DormandPrince853Integrator(minStep, maxStep, tolerances[0], tolerances[1]);

      numericalProp = new NumericalPropagator(integrator);
      numericalProp.setOrbitType(orbit.getType());

      provider = GravityFieldFactory.getNormalizedProvider(10, 10);

      holmesFeatherstone = new HolmesFeatherstoneAttractionModel(inertialFrame, provider);

      numericalProp.addForceModel(holmesFeatherstone);
      numericalProp.setMasterMode(del_t, new StepHandler());
      numericalProp.setInitialState(vehicleState);
      addEventHandlers(numericalProp);
      SpacecraftState finalState = numericalProp.propagate(new AbsoluteDate(initialDate,_period*2));
    }
    catch (OrekitException oke)
    {
      System.out.println(oke.getMessage());
    }
  }

  protected static class StepHandler implements OrekitFixedStepHandler
  {
    static String dataContainer = "";
    public void init(final SpacecraftState s0, final AbsoluteDate t)
    {
      System.out.println("          date                a           e" +
              "           i         \u03c9          \u03a9" + "          \u03bd");
    }

    public void handleStep(SpacecraftState currentState, boolean isLast)
    {
      KeplerianOrbit o = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(currentState.getOrbit());
      this.dataContainer = String.format(Locale.US, "%s %12.3f %10.8f %10.6f %10.6f %10.6f %10.6f%n",
              currentState.getDate(),
              o.getA(), o.getE(),
              Math.toDegrees(o.getI()),
              Math.toDegrees(o.getPerigeeArgument()),
              Math.toDegrees(o.getRightAscensionOfAscendingNode()),
              Math.toDegrees(o.getTrueAnomaly()));
      System.out.println(dataContainer);
      if (isLast)
      {
        //System.out.println(dataContainer);
        System.out.println();
      }
    }
  }

  protected static class DateHandler implements EventHandler<DateDetector>
  {
    ImpulseManeuver maneuver;
    public Action eventOccurred(final SpacecraftState s, final DateDetector detector, final boolean increasing) throws OrekitException
    {
      Double val = detector.g(s);
      Double threshold = detector.getThreshold();
      System.out.println("Date Handler Triggered! Detector Date: " + detector.getDate().toString());
      System.out.println("Difference between date and switch: " + val.toString());
      System.out.println("Current threshold: " + threshold.toString());
      return Action.CONTINUE;
    }

    public SpacecraftState resetState(final DateDetector detector, final SpacecraftState oldState)
    {
      return oldState;
    }
  }

}