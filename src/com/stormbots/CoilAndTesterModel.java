package com.stormbots;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by e on 1/18/19.
 */
public class CoilAndTesterModel {

    //270 degrees is 12000
    public final double max_voltage = 12000.0;
    public final double voltage_per_degree = max_voltage/270.0;
    protected double desired_dial_setting_in_degrees = 0.0;
    protected double current_dial_setting_in_degrees = 0.0;
    protected double random_noise_max_value_in_volts = 10.0;
    public final int samples_per_second = 10;
    public final int delay_in_seconds = 3;
    protected double current_value = 0;
    protected boolean stop = false;
    protected Thread dataThread = null;
    protected double dial_rate_of_change_in_degrees_per_second = 20.00;

    public double getCurrentVoltage() {
        return current_value;
    }

    protected double takeVoltageReading() {
        double voltage =  voltage_per_degree*current_dial_setting_in_degrees + ThreadLocalRandom.current().nextDouble(-1.0*random_noise_max_value_in_volts,
                random_noise_max_value_in_volts);
        if (voltage == Double.NaN) {
            System.out.println("What?");
        }
        if (voltage < 0) {
            System.out.println("WARNING: Model V < 0!, trimming.");
            voltage = 0;
        }

        return voltage;
    }

    private void processData() {
        double values[] = new double[delay_in_seconds*samples_per_second];
        int current_index = 0;
        long sleep_time_ms = 1000/samples_per_second;
        boolean dial_is_moving = false;
        long last_iteration_time = System.currentTimeMillis();

        while (!stop) {
            if (Math.abs(current_dial_setting_in_degrees - desired_dial_setting_in_degrees) > 0.0) {
                if (!dial_is_moving) {
                    dial_is_moving = true;
                    last_iteration_time = System.currentTimeMillis();
                }
                else {
                    //we need to move the dial
                    long current_time_delta_ms = System.currentTimeMillis() - last_iteration_time;
                    if (current_time_delta_ms > 250) {
                        double current_adjustment_amount_available_degrees = dial_rate_of_change_in_degrees_per_second * ((double)current_time_delta_ms / 1000.0);

                        double delta = desired_dial_setting_in_degrees - current_dial_setting_in_degrees;
                        //determine direction
                        double direction = (delta < 0) ? -1 : 1;
                        if (Math.abs(delta) < current_adjustment_amount_available_degrees)
                            current_dial_setting_in_degrees = desired_dial_setting_in_degrees;
                        else {
                            current_dial_setting_in_degrees += (direction * current_adjustment_amount_available_degrees);
                        }
                        last_iteration_time = System.currentTimeMillis();
                    }
                }

            }
            else {
                dial_is_moving = false;
            }

            if (current_index >= values.length) current_index = 0;
            values[current_index++] = takeVoltageReading();
            current_value = Arrays.stream(values).sum()/values.length;
            try {
                Thread.sleep(sleep_time_ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
    }

  public void start() {
      stop = false;
      dataThread = new Thread(() -> processData());
      dataThread.start();
  }

  public synchronized  void stop() {
      if (stop != true) {
          stop = true;
          try {
              if (dataThread != null) {
                  dataThread.interrupt();
                  dataThread.join(3000);
                  dataThread = null;
              }
              else {
                  System.out.println("DATA THREAD NULL");
              }

          } catch (InterruptedException e) {
              e.printStackTrace();
          }
      }
  }

  public void setDialPosition(double degrees) {
      if (degrees > 360) {
          System.out.println("WARNING: Degrees greater than 360. Trimming");
          degrees = 360;
      }
      else {
          if (degrees <0) {
              System.out.println("WARNING: Degrees less than 0.  Trimming.");
              degrees = 0;
          }
      }
      //todo this isn't instantaneous, model differently later.
      desired_dial_setting_in_degrees = degrees;
  }

  public double getDialPosition() { return current_dial_setting_in_degrees;}

  public double getDialSetPosition() {return desired_dial_setting_in_degrees;}

  public static void main(String[] args) throws InterruptedException {
      CoilAndTesterModel model = new CoilAndTesterModel();
      model.start();
      Random random = new Random();


      while(true) {

          if (model.getDialPosition() == model.getDialSetPosition()) {
              System.out.println("Setting new position");
              model.setDialPosition(random.nextInt((360 - 0) + 1) + 0);
          }
          else {
              Thread.sleep(2000);
          }

          System.out.println("Dial Position: "+model.getDialPosition()+"\tSet Point: "+model.getDialSetPosition());

      }
  }

}
