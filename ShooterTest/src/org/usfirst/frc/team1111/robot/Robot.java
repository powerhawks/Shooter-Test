package org.usfirst.frc.team1111.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;

/**
 * This code will test the shooter mounted to Peregrine.
 * It is controlled solely by the OPERATOR and is pre-configured for Xander's preferences (see ENB for details)
 * In teleopPeriodic(), there are two blocks of logic, one for single button shooting and one for dual button shooting.
 * Single button shooting automatically pushes the box into the spun up motors when they are up to speed.
 * Dual button shooting spins up the motors but relies on the OPERATOR to push the box in.
 * In both cases, a boolean box on the SmartDashboard will display if the motors are spun up or not.
 * Use the ControlChooser on the SmartDashboard to switch between single and dual button shooter.
 * The PID values for the shooting motors are used to adjust their acceleration curve for a faster spinup and maintain a certain speed.
 * These values are already calibrated.
 * This code also includes in testPeriodic() a block of logic intended to test that all of the motors are spinning the right way.
 * 
 * =====!!!!!*****PERFORM THE TESTS FIRST TO VERIFY EVERYTHING IS CONFIGURED PROPERLY*****!!!!!=====
 * 
 * These tests have to be run in TEST PERIODIC mode in the DriverStation.
 * @author Braidan
 *
 */
public class Robot extends IterativeRobot {
	Joystick joy = new Joystick(0);
	
	DigitalInput boxSensor = new DigitalInput(0);
	
	TalonSRX shooterFrontLeft = new TalonSRX(1111); //TODO: Configure
	TalonSRX shooterFrontRight = new TalonSRX(1111); //TODO: Configure
	TalonSRX shooterBackLeft = new TalonSRX(1111); //TODO: Configure
	TalonSRX shooterBackRight = new TalonSRX(1111); //TODO: Configure
	TalonSRX intakeLeft = new TalonSRX(1111); //TODO: Configure
	TalonSRX intakeRight = new TalonSRX(1111); //TODO: Configure
	final int TIMEOUT_DELAY = 200; //ms
	
	double scaleSpeed;
	int scaleVelocity = 60000; //TODO: Verify that is speed at 46% power and configure
	double scalePercent = .46; //TODO: Verify and configure
	
	double switchSpeed;
	int switchVelocity = 1111; //TODO: configure
	double switchPercent = .5; //TODO: Verify and configure
	
	final double INTAKE_SPEED = 0.5; //TODO: Verify this is correct intake speed
	boolean spunUp = false;
	
	DoubleSolenoid shooterPiston = new DoubleSolenoid(1111, 1111); //TODO: Configure
	boolean scale = false;
	
	double p = 0.0000175, i = 0.000001, d = 0; //Tuned PID values
	MiniPID pid = new MiniPID(p, i, d);
	
	final static String CHOOSE_PERCENT = "Percent Output";
	final static String CHOOSE_VELOCITY = "Velocity Output";
	SendableChooser<String> motorChooser = new SendableChooser<>();
	String motorSelected = "";
	
	final static String CHOOSE_SINGLE = "Single Button";
	final static String CHOOSE_DUAL = "Double Button";
	SendableChooser<String> controlChooser = new SendableChooser<>();
	String controlSelected = "";
	boolean single;
	
	//=====BASIC METHODS=====
	
	@Override
	public void robotInit() {
		shooterBackLeft.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0, TIMEOUT_DELAY);
		shooterBackRight.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0, TIMEOUT_DELAY);
		
		motorChooser.addDefault("Percent", CHOOSE_PERCENT);
		motorChooser.addObject("Velocity", CHOOSE_VELOCITY);
		SmartDashboard.putData(motorChooser);
		
		controlChooser.addDefault("Single", CHOOSE_SINGLE);
		controlChooser.addObject("Dual", CHOOSE_DUAL);
		SmartDashboard.putData(controlChooser);
		
		SmartDashboard.putNumber("Scale Velocity:", scaleVelocity);
		SmartDashboard.putNumber("Switch Velocity:", switchVelocity);
		SmartDashboard.putNumber("Scale Percent:", scalePercent);
		SmartDashboard.putNumber("Switch Percent:", switchPercent);
		
		pid.setOutputLimits(-1, 1);
	}
	
	public void teleopInit() {
		scaleVelocity = (int) SmartDashboard.getNumber("Scale Velocity:", scaleVelocity);
		switchVelocity = (int) SmartDashboard.getNumber("Switch Velocity:", switchVelocity);
		scalePercent = SmartDashboard.getNumber("Scale Percent:", scalePercent);
		switchPercent = SmartDashboard.getNumber("Switch Percent:", switchPercent);
		
		//Determines which system to use for speed - VELOCITY or PERCENTAGE
		motorSelected = motorChooser.getSelected();
		switch (motorSelected) {
		case (CHOOSE_VELOCITY): 
			scaleSpeed = scaleVelocity;
			switchSpeed = switchVelocity;
			break;
		default:
			scaleSpeed = scalePercent;
			switchSpeed = switchPercent;
			break;
		}
		
		//Determines which system to use for control - SINGLE button or DUAL button
		controlSelected = controlChooser.getSelected();
		switch (controlSelected) {
		case (CHOOSE_DUAL): 
			single = false;
			break;
		default:
			single = true;
			break;
		}
	}

	@Override
	public void teleopPeriodic() {
		boolean boxPos = boxSensor.get();

		//===Intake logic===
		if (joy.getRawButton(2) && !boxPos) { //Press A to INTAKE - Will not work when box is in position
			setIntakes(INTAKE_SPEED);
		}
		else if (joy.getRawButton(3)) { //Press B to OUTTAKE
			setIntakes(-INTAKE_SPEED);
		}
		else {
			setIntakes(0);
		}
		
		//===SINGLE BUTTON shooter logic===
		if (boxPos && joy.getRawButton(5) && single) { //Press LB to shoot for SCALE
			shoot(scaleSpeed);
		}
		else if (boxPos && joy.getRawButton(6) && single) { //Press RB to shoot for SWTICH
			shoot(switchSpeed);
		}
		else {
			setShooters(0);
		}
		
		//===DUAL BUTTON shooter logic===
		if (boxPos && joy.getRawButton(5) && !single) { //Press LB to spin up for SCALE
			spinUp(scaleSpeed);
		}
		else if (boxPos && joy.getRawButton(6) && !single) { //Press RB to spin up for SWTICH
			spinUp(switchSpeed);
		}
		else {
			setShooters(0);
		}
		if (spunUp && joy.getRawButton(8) && !single) { //Press RT to push box into shooter
			setIntakes(1);
		}
		
		//===Piston logic===
		if (joy.getPOV() == 0) { //Press UP DPAD to raise shooter
			changeAngle(1);
		}
		else if (joy.getPOV() == 90) { //Press DOWN DPAD to lower shooter
			changeAngle(0);
		}
		
		//===Debug section===
		SmartDashboard.putNumber("Left Motor Speed:", shooterBackLeft.getSelectedSensorVelocity(0)); //Debug
		SmartDashboard.putNumber("Right Motor Speed:", shooterBackRight.getSelectedSensorVelocity(0)); //Debug
		SmartDashboard.putBoolean("Box in Place:", boxPos); //Debug
	}

	@Override
	public void testPeriodic() {
		if (joy.getRawButton(1)) {
			shooterFrontLeft.set(ControlMode.PercentOutput, .25);
		}
		else if (joy.getRawButton(2)) {
			shooterFrontRight.set(ControlMode.PercentOutput, .25);
		}
		else if (joy.getRawButton(3)) {
			shooterBackLeft.set(ControlMode.PercentOutput, .25);
		}
		else if (joy.getRawButton(4)) {
			shooterBackRight.set(ControlMode.PercentOutput, .25);
		}
		else if (joy.getRawButton(5)) {
			intakeLeft.set(ControlMode.PercentOutput, .25);
		}
		else if (joy.getRawButton(6)) {
			intakeRight.set(ControlMode.PercentOutput, .25);
		}
		else if (joy.getPOV() == 0) { //Press UP DPAD to raise shooter
			changeAngle(1);
		}
		else if (joy.getPOV() == 90) { //Press DOWN DPAD to lower shooter
			changeAngle(0);
		}
		else {
			setIntakes(0);
			setShooters(0);
		}
	}
	
	//=====SHOOTER METHODS=====
	
	private void shoot(double target) {
		spinUp(target);
		
		if (spunUp) { //If motors are spun up, push box into shooter
			setIntakes(1);
		}
	}
	
	private void spinUp(double target) {
		double curSpeed;
		
		//Checks and automatically switches to working encoder to get velocity data. Defaults to LEFT side
		try {
			curSpeed = shooterBackLeft.getSelectedSensorVelocity(0);
		}
		catch (Exception exception) {
			curSpeed = shooterBackRight.getSelectedSensorVelocity(0);
		}
		finally {
			curSpeed = 0;
			System.out.println("=====FAILURE TO READ FROM ANY ENCODER=====");
		}
		
		if (inRange(target, 0, 1)) { //Checks for motor percentage
			setShooters(target);
		}
		else { //Motor velocity
			pid.setSetpoint(target);
			double speed = pid.getOutput(curSpeed);
			setShooters(speed);
			
			if (inRange(curSpeed, target, 1000)) { //Checks if in range of target velocity +/- 1000 units
				spunUp = true;
			}
		}
		
		SmartDashboard.putBoolean("Motors Spun Up:", spunUp); //Will always display for drive team - NOT A DEBUG
	}
	
	//=====PISTON METHODS=====
	
	private void changeAngle(int state) {
		if (state == 0) { //LOWERS piston
			shooterPiston.set(Value.kReverse);
		}
		else if (state == 1) { //RAISES piston
			shooterPiston.set(Value.kForward);
		}
	}
	
	//=====UTILITY METHODS=====
	
	private boolean inRange(double x, double target, double bound) {
		return x < target+bound && x > target-bound;
	}
	
	private void setShooters(double speed) {
		shooterFrontLeft.set(ControlMode.PercentOutput, speed); //TODO: Verify and configure +/- speed
		shooterFrontRight.set(ControlMode.PercentOutput, -speed); //TODO: Verify and configure +/- speed
		shooterBackLeft.set(ControlMode.PercentOutput, speed); //TODO: Verify and configure +/- speed
		shooterBackRight.set(ControlMode.PercentOutput, -speed); //TODO: Verify and configure +/- speed
	}
	
	private void setIntakes(double speed) {
		intakeLeft.set(ControlMode.PercentOutput, speed); //TODO: Verify and configure +/- speed
		intakeRight.set(ControlMode.PercentOutput, -speed); //TODO: Verify and configure +/- speed
	}
}
