// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.function.DoubleSupplier;

import com.ctre.phoenix.sensors.WPI_Pigeon2;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.commands.Drive;
import frc.robot.subsystems.Drivetrain;

public class RobotContainer {
  public final Drivetrain drive = Drivetrain.getInstance();
  private final CommandXboxController driveController = new CommandXboxController(0);
  private final double deadzone = 0.3;

  public RobotContainer() {
    configureBindings();
    drive.setDefaultCommand(
        // If we're not moving, rotate. Otherwise, drive
        // new ConditionalCommand(
        //     new Rotate(
        //         () -> applyJoystickDeadzone(driveController.getRightX(), deadzone)),
        //     new Drive(
        //         () -> applyJoystickDeadzone(driveController.getRightX(), deadzone),
        //         () -> applyJoystickDeadzone(driveController.getLeftY(), deadzone)),
        //     () -> applyJoystickDeadzone(driveController.getLeftY(), deadzone) == 0)
        new Rotate(() -> applyJoystickDeadzone(driveController.getRightX(), deadzone)));
  }

  private void configureBindings() {
    Trigger zeroSensorsButton = driveController.a();
    zeroSensorsButton.onTrue(new ZeroDriveSensors());
    Trigger zeroHeadingButton = driveController.b();
    zeroHeadingButton.onTrue(new RotateToHeading(0));
  }

  public Command getAutonomousCommand() {
    return Commands.print("No autonomous command configured");
  }

    private static DoubleSupplier deadzoneModifier(DoubleSupplier rawValueSupplier, double deadzone) {
        return () -> {
            double rawValue = rawValueSupplier.getAsDouble();
            double result;
            double validRange = 1 - deadzone;
            double value = Math.abs(rawValue);

            if (value > deadzone) {
                result = (value - deadzone) / validRange;
            } else {
                result = 0;
            }

            return rawValue < 0 ? -result : result;
        };
    }
}
