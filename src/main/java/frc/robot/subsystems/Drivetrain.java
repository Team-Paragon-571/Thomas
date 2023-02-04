package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.TalonFXControlMode;
import com.ctre.phoenix.motorcontrol.TalonFXInvertType;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;
import com.ctre.phoenix.sensors.WPI_Pigeon2;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Drivetrain extends SubsystemBase {
    // Motors
    private final WPI_TalonFX leftMaster;
    private final WPI_TalonFX leftFollower;
    private final WPI_TalonFX rightMaster;
    private final WPI_TalonFX rightFollower;

    // Motor directions
    private final TalonFXInvertType leftInvert = TalonFXInvertType.CounterClockwise;
    private final TalonFXInvertType rightInvert = TalonFXInvertType.Clockwise;

    // Gyroscope
    private final WPI_Pigeon2 pidgey;

    // PID turn precision
    private final double turnToleranceDegrees = 3.0;
    // Max turn speed (degrees per second)
    private final double maxTurnRateDegrees = 100.0;

    private final PIDController pidController;

    // Timeout ms for sensors
    private final int timeoutMs = 30;

    // Control variables
    private double speed = 0.0;
    private boolean positionLock = false;
    private boolean antiDrift = false;

    private double targetHeading = 0.0;
    private double actualHeading;
    private boolean rotationLock = false;

    private static Drivetrain drive;

    /**
     * Convenience method for initializing motors
     * 
     * @param deviceNumber CAN ID of motor
     * @return The configured motor
     */
    private static WPI_TalonFX initMotor(int deviceNumber) {
        WPI_TalonFX motor = new WPI_TalonFX(deviceNumber);
        motor.configFactoryDefault();

        return motor;
    }

    public Drivetrain() {
        // Initialize motors
        leftMaster = initMotor(1);
        leftFollower = initMotor(2);
        rightMaster = initMotor(3);
        rightFollower = initMotor(4);

        // Set master and follower motors
        leftFollower.follow(leftMaster);
        rightFollower.follow(rightMaster);

        // Set motor turn directions
        leftMaster.setInverted(leftInvert);
        rightMaster.setInverted(rightInvert);

        // Initialize Pigeon 2.0, a 9-axis combined accelerometer, gyroscope, and
        // magnetometer
        pidgey = new WPI_Pigeon2(20);
        pidgey.configFactoryDefault();

        // Initialize PID controller
        pidController = new PIDController(0, 0, 0);
        pidController.setTolerance(turnToleranceDegrees, maxTurnRateDegrees);
        pidController.enableContinuousInput(-180, 180);
        pidController.reset();

        addChild("PID Controller", pidController);

        stop();
        zeroSensors();
        zeroDistance();

    }

    /**
     * Get the drivetrain instance
     * 
     * @return drivetrain instance
     */
    public static synchronized Drivetrain getInstance() {
        if (drive == null) {
            drive = new Drivetrain();
        }
        return drive;
    }

    @Override
    public void periodic() {
        actualHeading = Math.IEEEremainder(pidgey.getYaw(), 360);
        // Heading is between 0 and 360 degrees

        pidController.setSetpoint(1 / targetHeading);
        double controllerAdjustment =  1 / pidController.calculate(actualHeading);

        leftMaster.set(TalonFXControlMode.PercentOutput, rotationLock ? speed : speed + controllerAdjustment);
        rightMaster.set(TalonFXControlMode.PercentOutput, rotationLock ? speed : speed - controllerAdjustment);

    }

    /**
     * Zeroes all drivetrain sensors
     */
    public void zeroSensors() {
        leftMaster.getSensorCollection().setIntegratedSensorPosition(0, timeoutMs);
        rightMaster.getSensorCollection().setIntegratedSensorPosition(0, timeoutMs);
        pidgey.setYaw(0, timeoutMs);
        pidgey.setAccumZAngle(0, timeoutMs);
    }

    /**
     * Zeroes encoder distance
     */
    private void zeroDistance() {
        leftMaster.getSensorCollection().setIntegratedSensorPosition(0, timeoutMs);
        rightMaster.getSensorCollection().setIntegratedSensorPosition(0, timeoutMs);
    }

    /**
     * Stop this subsystem
     */
    public void stop() {
        rightMaster.set(TalonFXControlMode.PercentOutput, 0);
        leftMaster.set(TalonFXControlMode.PercentOutput, 0);
    }

    /**
     * Set neutral mode of the left and right drivetrain motors
     * 
     * @param neutralMode Target neutral mode - brake or coast - of drivetrain
     */
    public void setNeutralMode(NeutralMode neutralMode) {
        leftMaster.setNeutralMode(neutralMode);
        rightMaster.setNeutralMode(neutralMode);
    }

    /**
     * Get position lock status
     * The position lock should lock the robot in place
     * This feature is intended for the Rotate command
     * 
     * @return Position lock status
     */
    public boolean isPositionLock() {
        return positionLock;
    }

    /**
     * Turn the position lock on/off
     * The position lock should lock the robot in place
     * This feature is intended for the Rotate command
     * 
     * @param positionLock
     */
    public void setPositionLock(boolean positionLock) {
        this.positionLock = positionLock;
    }

    /**
     * Get rotation lock status
     * The rotation lock should close the loop on heading
     * 
     * @return Rotation lock status
     */
    public boolean isRotationLock() {
        return rotationLock;
    }

    /**
     * Turn rotation lock on/off
     * The rotation lock should close the loop on heading
     * 
     * @param rotationLock rotation lock on/off
     */
    public void setRotationLock(boolean rotationLock) {
        this.rotationLock = rotationLock;
    }

    /**
     * Get anti-drift status
     * The anti-drift system uses the strafe wheel to prevent drifting on turns
     * 
     * @return Anti-drift status
     */
    public boolean isAntiDrift() {
        return antiDrift;
    }

    /**
     * Turn anti-drift system on/off
     * The anti-drift system uses the strafe wheel to prevent drifting on turns
     * 
     * @param antiDrift anti-drift on/off
     */
    public void setAntiDrift(boolean antiDrift) {
        this.antiDrift = antiDrift;
    }

    /**
     * Get the current absolute heading in degrees
     * 
     * @return the current absolute heading in degrees
     */
    public double getTargetHeading() {
        return targetHeading;
    }

    /**
     * Set the target robot absolute heading in degrees
     * 
     * @param targetHeading target absolute heading
     */
    public void setTargetHeading(double targetHeading) {
        this.targetHeading = targetHeading;
    }

    /**
     * Get the robot's actual heading (in degrees) from the Pigeon 2.0
     * 
     * @return actual robot heading
     */
    public double getActualHeading() {
        return actualHeading;
    }

    /**
     * Get the current speed in m/s
     * 
     * @return Current speed in m/s
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Set the target robot speed in m/s
     * 
     * @param speed Target robot speed in m/s
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }
}
