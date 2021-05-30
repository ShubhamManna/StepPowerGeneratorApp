package com.example.steppowergenerator.ui.pedometer;

import androidx.cardview.widget.CardView;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.steppowergenerator.AccelerationData;
import com.example.steppowergenerator.R;
import com.example.steppowergenerator.StepDetector;
import com.example.steppowergenerator.StepListener;
import com.example.steppowergenerator.StepType;

import java.util.ArrayList;
import java.util.Locale;

import static android.content.Context.SENSOR_SERVICE;

public class pedometerFragment extends Fragment implements SensorEventListener, StepListener {

    private static final String TAG = "PedometerFragment";

    private CardView cardViewToggleStepCounting;
    private TextView textView_amount_steps, textView_type_of_step,
            textView_pedometer_is_running, textView_pedometer_toggle_text;

    // Results - text views
    private TextView textview_results_total_steps, textview_results_walking_steps, textview_results_jogging_steps, textview_results_running_steps,
            textview_results_total_distance, textview_results_average_speed, textview_results_burned_calories, textview_results_power_generated;

    // ViewModel - saves all relevant data here.
    private pedometerViewModel mViewModel;

    /**
     * Returns a new instance of the step counter fragment.
     *
     * @return new instance
     */
    public static pedometerFragment newInstance() {
        return new pedometerFragment();
    }

    /**
     * Is called when the view / GUI is created.
     * Returns the view
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return View - newly generated GUI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pedometer_fragment, container, false);

        cardViewToggleStepCounting = view.findViewById(R.id.btn_pedometer_toggle_tracking);
        cardViewToggleStepCounting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mViewModel.isCountingSteps()) stopCounting();
                else startCounting();
            }
        });
        textView_pedometer_toggle_text = view.findViewById(R.id.textview_pedometer_toggle_text);

        textView_amount_steps = view.findViewById(R.id.textview_amount_steps);
        textView_type_of_step = view.findViewById(R.id.textview_pedometer_type_of_step);
        textView_pedometer_is_running = view.findViewById(R.id.textview_pedometer_isRunning);

        textview_results_total_steps = view.findViewById(R.id.textview_results_total_steps);
        textview_results_walking_steps = view.findViewById(R.id.textview_results_walking_steps);
        textview_results_jogging_steps = view.findViewById(R.id.textview_results_jogging_steps);
        textview_results_running_steps = view.findViewById(R.id.textview_results_running_steps);
        textview_results_total_distance = view.findViewById(R.id.textview_results_total_distance);
        textview_results_average_speed = view.findViewById(R.id.textview_results_average_speed);
        textview_results_burned_calories = view.findViewById(R.id.textview_results_burned_calories);
        textview_results_power_generated = view.findViewById(R.id.textview_results_power_generated);

        if (mViewModel.getSensorManager() == null) {
            mViewModel.setSensorManager((SensorManager) getActivity().getSystemService(SENSOR_SERVICE));
        }
        if (mViewModel.getAccelerationSensor() == null) {
            if (mViewModel.getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                mViewModel.setAccelerationSensor(mViewModel.getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            }
        }
        if (mViewModel.getStepDetector() == null) {
            mViewModel.setStepDetector(new StepDetector());
        }
        mViewModel.getStepDetector().registerStepListener(this);

        if (mViewModel.getAccelerationDataArrayList() == null) {
            mViewModel.setAccelerationDataArrayList(new ArrayList<AccelerationData>());
        }

        if (mViewModel.isCountingSteps()) {
            textView_pedometer_toggle_text.setText(getResources().getText(R.string.disable_pedometer));
            textView_pedometer_is_running.setText(getResources().getText(R.string.pedometer_running));
            textView_pedometer_is_running.setTextColor(getResources().getColor(R.color.green));
            textView_amount_steps.setText(String.valueOf(mViewModel.getAmountOfSteps()));
            mViewModel.getSensorManager().registerListener(this, mViewModel.getAccelerationSensor(), SensorManager.SENSOR_DELAY_NORMAL);
        }
        return view;
    }

    /**
     * Is called when the fragment is created. Initializes the ViewModel.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(pedometerViewModel.class);
        super.onCreate(savedInstanceState);
    }

    /**
     * Called before the fragment is destroyed.
     */
    @Override
    public void onDetach() {
        mViewModel.getSensorManager().unregisterListener(this);
        super.onDetach();
    }

    /**
     * Is called when the measured values ​​of a sensor change.
     * Since only the acceleration sensor is registered, values ​​only come from this.
     *
     * @param sensorEvent SensorEvent with all new measured values, time stamp and origin
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        AccelerationData newAccelerationData = new AccelerationData();
        newAccelerationData.setX(sensorEvent.values[0]);
        newAccelerationData.setY(sensorEvent.values[1]);
        newAccelerationData.setZ(sensorEvent.values[2]);
        newAccelerationData.setTime(sensorEvent.timestamp);

        mViewModel.getAccelerationDataArrayList().add(newAccelerationData);
        mViewModel.getStepDetector().addAccelerationData(newAccelerationData);

        // Previous version (now handled in StepDetector):
        /*
        // at 200 millisecond delay approx. 5 seconds
        if (mViewModel.getAccelerationDataArrayList (). size ()> = 25) {
            sendDataArray ();
        } */
    }

    /*
    private void sendDataArray(){
        mViewModel.getStepDetector().handleData(mViewModel.getAccelerationDataArrayList());
        mViewModel.getAccelerationDataArrayList().clear();
    }*/

    /**
     * Called when the accuracy of the registered sensor changes.
     *
     * @param sensor Sensor of which the accuracy has changed.
     * @param i new accuracy
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * Is called when a step has been recognized in the StepDetector. Saves the step in the ViewModel.
     *
     * @param accelerationData AccelerationData: A data record of the acceleration sensor, which stands for a step.
     * @param stepType Enum StepType: One of the three step types from the Enum StepType.
     */
    @Override
    public void step(AccelerationData accelerationData, StepType stepType) {
        // Step event coming back from StepDetector
        mViewModel.setAmountOfSteps(mViewModel.getAmountOfSteps() + 1);
        textView_amount_steps.setText(String.valueOf(mViewModel.getAmountOfSteps()));
        if (stepType == StepType.WALKING) {
            mViewModel.setWalkingSteps(mViewModel.getWalkingSteps() + 1);
            textView_type_of_step.setText(getResources().getText(R.string.walking));
        } else if (stepType == StepType.JOGGING) {
            mViewModel.setJoggingSteps(mViewModel.getJoggingSteps() + 1);
            textView_type_of_step.setText(getResources().getText(R.string.jogging));
        } else {
            mViewModel.setRunningSteps(mViewModel.getRunningSteps() + 1);
            textView_type_of_step.setText(getResources().getText(R.string.running));
        }
    }

    /**
     * Calculates results of the last measurement. Estimates only.
     * Are shown in the GUI.
     */
    private void calculateResults() {
        int totalSteps = mViewModel.getAmountOfSteps();
        textview_results_total_steps.setText(String.valueOf(totalSteps));

        int walkingSteps = mViewModel.getWalkingSteps();
        int joggingSteps = mViewModel.getJoggingSteps();
        int runningSteps = mViewModel.getRunningSteps();

        textview_results_walking_steps.setText(String.valueOf(walkingSteps));
        textview_results_jogging_steps.setText(String.valueOf(joggingSteps));
        textview_results_running_steps.setText(String.valueOf(runningSteps));

        float totalDistance = walkingSteps * 0.5f + joggingSteps * 1.0f + runningSteps * 1.5f;
        String distance = totalDistance + " m";
        textview_results_total_distance.setText(distance);

        float totalDuration = walkingSteps * 1.0f + joggingSteps * 0.75f + runningSteps * 0.5f;
        float hours = totalDuration / 3600;
        float minutes = (totalDuration % 3600) / 60;
        float seconds = totalDuration % 60;
        String duration = String.format(Locale.ENGLISH, "%.0f", hours) + "h " +
                String.format(Locale.ENGLISH, "%.0f", minutes) + "min " +
                String.format(Locale.ENGLISH, "%.0f", seconds) + "s";

        // Average speed:
        String averageSpeed = String.format(Locale.ENGLISH, "%.2f", totalDistance / totalDuration) + " m/s";
        textview_results_average_speed.setText(averageSpeed);

        // Calories
        float totalCaloriesBurned = walkingSteps * 0.05f + joggingSteps * 0.1f + runningSteps * 0.2f;
        String totalCalories = String.format(Locale.ENGLISH, "%.0f", totalCaloriesBurned) + " Calories";
        textview_results_burned_calories.setText(totalCalories);

        //Power generated
        float PowerGenerated = walkingSteps * 0.4542f + joggingSteps * 0.47f + runningSteps * 0.51f;
        String totalPower = String.format(Locale.ENGLISH, "%.0f", PowerGenerated) + "Watts";
        textview_results_power_generated.setText(totalPower);
    }

    /**
     * Resets some data.
     */
    private void resetUI() {
        mViewModel.setAmountOfSteps(0);
        mViewModel.setWalkingSteps(0);
        mViewModel.setJoggingSteps(0);
        mViewModel.setRunningSteps(0);
        textView_amount_steps.setText(String.valueOf(mViewModel.getWalkingSteps()));
    }

    /**

     * Starts step sensor. (The fragment is registered in the SensorManager)
     */
    private void startCounting() {
        if (!mViewModel.isCountingSteps()) {
            try {
                resetUI();
                mViewModel.getSensorManager().registerListener(this, mViewModel.getAccelerationSensor(), SensorManager.SENSOR_DELAY_NORMAL);
                mViewModel.setCountingSteps(true);
                textView_pedometer_toggle_text.setText(getResources().getText(R.string.disable_pedometer));
                textView_pedometer_is_running.setText(getResources().getText(R.string.pedometer_running));
                textView_pedometer_is_running.setTextColor(getResources().getColor(R.color.green));
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * Stops step sensor. (The fragment is not registered in the SensorManager)
     */
    private void stopCounting() {
        if (mViewModel.isCountingSteps()) {
            try {
                // The last remaining data is also processed
                // sendDataArray ();

                mViewModel.getSensorManager().unregisterListener(this);
                mViewModel.setCountingSteps(false);
                calculateResults();
                textView_pedometer_toggle_text.setText(getResources().getText(R.string.acitvate_pedometer));
                textView_pedometer_is_running.setText(getResources().getText(R.string.pedometer_not_running));
                textView_pedometer_is_running.setTextColor(getResources().getColor(R.color.red));
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }
    }
}
