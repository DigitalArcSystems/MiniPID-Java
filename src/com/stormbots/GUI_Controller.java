package com.stormbots;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Slider;

import java.util.Arrays;

/**
 * Created by e on 1/18/19.
 */
public class GUI_Controller {

    @FXML
    Slider pSlider;

    @FXML
    Slider iSlider;

    @FXML
    Slider dSlider;

    @FXML
    Slider tSlider;

    @FXML
    Slider ruSlider;

    @FXML
    Slider filterSlider;


    @FXML
    Slider maxOutSlider;

    @FXML
    LineChart graph;

    @FXML
    StackedBarChart dialPosition;

    private CoilAndTesterModel model = null;
    private MiniPID pid = null;
    private boolean stop = true;
    private LineChart.Series targetSeries;
    private LineChart.Series actualSeries;
    private LineChart.Series outputSeries;
    private LineChart.Series errorSeries;
    private BarChart.Series  dialPositionSeries_Bottom;
    private BarChart.Series  dialPositionSeries_Top;
    private BarChart.Series dialPositionSeries_Middle;
    private Thread simulationThread = null;
    private long time_between_runs_ms = 200;
    private double initialP = 0.03;//0.12;
    private double initialI = 0.01;//0.045;
    private double initialD = 0.15;//0.079;
    private double MAX_VOLTAGE = 12000;
    private double initialF = 270.0/MAX_VOLTAGE;
    private BarChart.Data lowerBar = new BarChart.Data<String, Double>("Dial",0.0);
    private BarChart.Data upperBar = new BarChart.Data<String, Double>("Dial",180.0);
    private BarChart.Data middleBar = new BarChart.Data<String, Double>("Dial",180.0);



    public void onStart() {
        if (stop) {
            stop = false;
            model.start();
            graph.getData().clear();
            graph.setCreateSymbols(false);
            targetSeries = new XYChart.Series<Double, Double>();
            targetSeries.setName("Target");
            outputSeries = new XYChart.Series<Double, Double>();
            outputSeries.setName("Output");
            actualSeries = new XYChart.Series<Double, Double>();
            actualSeries.setName("Actual");
            errorSeries  = new XYChart.Series<Double, Double>();
            errorSeries.setName("Error");
            dialPositionSeries_Bottom = new BarChart.Series<String, Double>();
            dialPositionSeries_Bottom.setName("Unchanged Position");
            dialPositionSeries_Top = new BarChart.Series<String, Double>();
            dialPositionSeries_Top.setName("Desired Position Positive");
            dialPositionSeries_Middle = new BarChart.Series<String, Double>();
            dialPositionSeries_Middle.setName("Desired Position Negative");

            dialPosition.getData().clear();
            dialPosition.getYAxis().setAutoRanging(false);
            ((CategoryAxis)dialPosition.getXAxis()).setCategories(FXCollections.<String>observableArrayList(Arrays.asList
                    ("Dial")));
            dialPositionSeries_Bottom.getData().add(lowerBar);
            dialPositionSeries_Middle.getData().add(middleBar);
            dialPositionSeries_Top.getData().add(upperBar);
            graph.getData().addAll(targetSeries, actualSeries, outputSeries, errorSeries);
            dialPosition.getData().addAll(dialPositionSeries_Bottom, dialPositionSeries_Middle, dialPositionSeries_Top);
            dialPosition.setAnimated(false);

            simulationThread = new Thread(() -> simulate());
            simulationThread.start();

        }

    }

    public void onStop() {
        if (!stop) {
            stop = true;
            try {
                model.stop();
                simulationThread.join(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void onReset() {
        onStop();
        onStart();
    }

    public void simulate() {

//        model = new CoilAndTesterModel();
//        pid = new MiniPID(pSlider.getValue(), iSlider.getValue(), dSlider.getValue());

        model.setDialPosition(0.0);
        while (model.getDialPosition() > 0) try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long initial_time = System.currentTimeMillis();
        pid = new MiniPID(initialP, initialI, initialD, initialF);
        pSlider.valueProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue arg0, Object arg1, Object arg2) {
                double value = pSlider.getValue();
                if (value == Double.NaN) {
                    System.out.println("Got one");
                    value = 0;
                }
                pid.setP(pSlider.getValue());

            }
        });

        iSlider.valueProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue arg0, Object arg1, Object arg2) {
                double value = iSlider.getValue();
                if (value == Double.NaN) {
                    System.out.println("Got one");
                    value = 0;
                }
                pid.setI(iSlider.getValue());

            }
        });

        dSlider.valueProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue arg0, Object arg1, Object arg2) {
                double value = dSlider.getValue();
                if (value == Double.NaN) {
                    System.out.println("Got one");
                    value = 0;
                }
                pid.setD(dSlider.getValue());

            }
        });


        double last_value = 0;
        double output = 0;
        while (!stop) {

            pid.setOutputRampRate(ruSlider.getValue());
            pid.setOutputFilter(filterSlider.getValue());

            double current_time_in_seconds = (System.currentTimeMillis()-initial_time)/1000.0;
            double actual = model.getCurrentVoltage();
            double target =  tSlider.getValue();

            double last_cross = 0;
            double max_value = 0;
            double dialDesired = model.getDialSetPosition();
            double dialActual = model.getDialPosition();
            double dialDelta = dialDesired - dialActual;
            final double lower_bar;
            final double upper_bar;
            final double middle_bar;
            if (dialDelta < 0) {
                lower_bar = dialDesired;
                upper_bar = 0;
                middle_bar = -1*dialDelta;
            }
            else {
                lower_bar = dialActual;
                upper_bar = dialDelta;
                middle_bar = 0;
            }

            if (dialDelta == 0) {
                pid.setOutputLimits(0.0, maxOutSlider.getValue());
                output = pid.getOutput(actual, target);
                model.setDialPosition(output);
            }

            double error = target - actual;
            final double output_for_now = output;
            Platform.runLater(() -> {
                targetSeries.getData().add(new XYChart.Data<Double, Double>(current_time_in_seconds, target));
                actualSeries.getData().add(new XYChart.Data<Double, Double>(current_time_in_seconds, actual));
                outputSeries.getData().add(new XYChart.Data<Double, Double>(current_time_in_seconds, output_for_now));
                errorSeries.getData().add(new XYChart.Data<Double, Double>(current_time_in_seconds, error));
                lowerBar.setYValue(lower_bar);
                upperBar.setYValue(upper_bar);
                middleBar.setYValue(middle_bar);
            });
            //System.out.println("==========================");
            //System.out.printf("Current: %3.2f , Actual: %3.2f, Error: %3.2f\n",actual, output, (target-actual));
            double p = pSlider.getValue();
            double i = iSlider.getValue();
            double d = dSlider.getValue();
            System.err.printf("%3.2f\t%3.2f\t%3.2f\t%3.2f\t%3.2f\t%3.2f\t%3.2f\tP:%3.2f\tI:%3.2f\tD:%3.2f\n", current_time_in_seconds, target, actual, output, dialActual, dialDelta, error,
                    p,i,d);
            if (actual > max_value) max_value = actual;
            if (last_value * error < 0) {

                System.out.println("CROSSING: "+current_time_in_seconds+"\tMAX_VALUE: "+max_value);

                max_value = 0;
                last_cross = current_time_in_seconds;

            }
            last_value = error;
            try {
                Thread.sleep(time_between_runs_ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    public void initialize() {

        model = new CoilAndTesterModel();
//        pid = new MiniPID(.2, 0, 0);
//        pid.setOutputLimits(0.0, 360.0);
        maxOutSlider.setValue(360.0);
//        pid.setOutputRampRate(5.0);
        ruSlider.setValue(10.0);
//        pid.setOutputFilter(0.1);
        filterSlider.setValue(0.1);
        pSlider.setValue(initialP);
        iSlider.setValue(initialI);
        dSlider.setValue(initialD);




        graph.getYAxis().setAutoRanging(true);
        graph.getXAxis().setAutoRanging(true);

    }
}
