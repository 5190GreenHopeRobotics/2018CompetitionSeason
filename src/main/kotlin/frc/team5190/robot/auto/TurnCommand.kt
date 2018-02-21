package frc.team5190.robot.auto

import com.ctre.phoenix.motorcontrol.ControlMode
import edu.wpi.first.wpilibj.command.PIDCommand
import frc.team5190.robot.drive.DriveSubsystem
import frc.team5190.robot.sensors.NavX
import frc.team5190.robot.vision.VisionSubsystem

/**
 * Command that turns the robot to a certain angle
 * @param angle Angle to turn to in degrees
 */
class TurnCommand(val angle: Double, val visionCheck: Boolean = false, val tolerance: Double = 0.0) : PIDCommand(0.08, 0.001, 0.1) {

    init {
        requires(DriveSubsystem)
        requires(VisionSubsystem)
    }

    override fun initialize() {
        // Only execute the command for a total of a max of 5 seconds (should be close enough to target by then)
        setTimeout(2.5)
        setName("DriveSystem", "RotateController")

        when (visionCheck) {
            false -> setpoint = angle
            true -> {
                when (VisionSubsystem.isTgtVisible == 1L) {
                    false -> {
                        println("Vision subsystem did not find any target object")
                        setpoint = angle
                    }
                    true -> {
                        val x = NavX.pidGet()                   // current absolute angle
                        val y = x + (VisionSubsystem.tgtAngle + VisionSubsystem.rawAngle) / 2.0    // Vision absolute angle
                        // (y - angle) is correction and it should be less than tolerance
                        setpoint = if (Math.abs(y - angle) < tolerance) {
                            println("Vision subsystem corrected $angle to $y")
                            y
                        } else {
                            println("Vision subsystem found object at $y instead of $angle, and it was outside tolerance range")
                            angle
                        }
                    }
                }
            }
        }

        setInputRange(-180.0, 180.0)
        pidController.setOutputRange(-0.8, 0.8)
        pidController.setAbsoluteTolerance(5.0)
        pidController.setContinuous(true)
    }

    override fun usePIDOutput(output: Double) = DriveSubsystem.falconDrive.tankDrive(ControlMode.PercentOutput, output, -output)

    override fun returnPIDInput(): Double = NavX.pidGet()

    private var time = 0

    override fun isFinished(): Boolean {
        if (pidController.onTarget()) {
            time++
        } else {
            time = 0
        }
        return (time > 500 / 20) || isTimedOut
    }
}