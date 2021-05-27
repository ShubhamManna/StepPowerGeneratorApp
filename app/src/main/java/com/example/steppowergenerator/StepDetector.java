package com.example.steppowergenerator;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 The StepDetector class is used to determine steps
 from data sets of the acceleration sensor. Via the StepListener interface
 the recognized steps are "returned".
 */
public class StepDetector {

    private static final int WALKINGTHRESHOLD = 17;
    private static final int JOGGINGTHRESHOLD = 24;
    private static final int RUNNINGTHRESHOLD = 30;

    private StepListener stepListener;

    private ArrayList<AccelerationData> newAccelerationDataList;
    private ArrayList<AccelerationData> calculatedList;

    /**
     * Class constructor.
     * Two empty array lists are created,
     * which are required in the other methods.
     */
    public StepDetector(){
        newAccelerationDataList = new ArrayList<>();
        calculatedList = new ArrayList<>();
    }


    /**
     * The registerStepListener method registers the given interface
     * as an attribute in the class. Steps recognized later are communicated via this.
     * @param pStepListener The interface which is notified of recognized steps.
     */
    public void registerStepListener(StepListener pStepListener){
        stepListener = pStepListener;
    }

    /**
     * The addAccelerationData method accepts new measured values ​​from the acceleration sensor.
     * If there are 25 records, they will be processed and 25 records will be collected again.
     * @param pNewAccelerationData
     */
    public void addAccelerationData(AccelerationData pNewAccelerationData){
        newAccelerationDataList.add(pNewAccelerationData);

        if(newAccelerationDataList.size() >= 25){
            handleAccelerationData();
        }
    }

    /**
     * The handleAccelerationData method recognizes steps in acceleration data.
     * The four methods calculateValueAndTime, findHighPoints, removeNearHighPoints, and examineStepTypeAndSendResponse
     * used. The vector length (= speed at a certain point in time) is also calculated for each data record and
     * the time attribute of the data record is changed from nanoseconds since device start time to Unix time (milliseconds).
     * After processing all data, the recognized steps are output via the interface and
     * emptied the array lists so that they can be used again.
     */
    private void handleAccelerationData(){

        for (int i = 0; i < newAccelerationDataList.size(); i++) {
            AccelerationData accelerationData = newAccelerationDataList.get(i);
            accelerationData = calculateValueAndTime(accelerationData);
            calculatedList.add(accelerationData);
        }

        ArrayList<AccelerationData> highPointList = findHighPoints();
        highPointList = removeNearHighPoints(highPointList);
        examineStepTypeAndSendResponse(highPointList);

        calculatedList.clear();
        newAccelerationDataList.clear();
    }

    /**
     * The method calculateValueAndTime calculates the vector length and the Unix timestamp for pAccelerationData.
     * The corresponding values ​​are changed in pAccelerationData. Then pAccelerationData is returned.
     * @param pAccelerationData Object from which the vector length and the Unix timestamp are calculated.
     * @return AccelerationData: The object with changed values.
     */
    private AccelerationData calculateValueAndTime(AccelerationData pAccelerationData){

        float x = pAccelerationData.getX();
        float y = pAccelerationData.getY();
        float z = pAccelerationData.getZ();

        double vectorLength = Math.sqrt(x * x + y * y + z * z);
        pAccelerationData.setValue(vectorLength);

        long time = pAccelerationData.getTime();
        long timeOffsetToUnix = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        long unixTimestamp = (time / 1000000L) + timeOffsetToUnix;
        pAccelerationData.setTime(unixTimestamp);

        return pAccelerationData;
    }

    /**
     * The findHighPoints method finds the acceleration data records from rawAccelerationData
     * whose total acceleration is higher than the value of WALKINGTHRESHOLD (17). These will
     * added to another ArrayList, which is returned.
     * @return ArrayList: A list with the highest high points.
     */
    private ArrayList<AccelerationData> findHighPoints(){
        ArrayList<AccelerationData> highPointList = new ArrayList<>();
        ArrayList<AccelerationData> aboveWalkingThresholdList = new ArrayList<>();
        boolean wasAboveThreshold = true;
        for (int i = 0; i < calculatedList.size(); i++) {

            AccelerationData calculatedDataSet = calculatedList.get(i);
            if(calculatedDataSet.getValue() > WALKINGTHRESHOLD){
                aboveWalkingThresholdList.add(calculatedDataSet);
                wasAboveThreshold = true;
            } else {
                // erst, wenn es einen Wert unter WALKINGTHRESHOLD gibt
                if(wasAboveThreshold && aboveWalkingThresholdList.size() > 0){
                    Collections.sort(aboveWalkingThresholdList, new AccelerationDataSorter());
                    highPointList.add(aboveWalkingThresholdList.get(aboveWalkingThresholdList.size() - 1));
                    aboveWalkingThresholdList.clear();
                }
                wasAboveThreshold = false;
            }
        }
        return highPointList;
    }


    /**
     * The method removeNearHighPoints goes the ArrayList pAccelerationData with the highest high points
     * and checks whether there is another "highest peak" within 400 milliseconds.
     * If so, the smaller of the two will be removed from the list.
     * @param pAccelerationDataList The list with the high points as an ArrayList
     * @return An ArrayList with the high points removed within 400 milliseconds
     */
    private ArrayList<AccelerationData> removeNearHighPoints(ArrayList<AccelerationData> pAccelerationDataList){
        ArrayList<Integer> wrongHighPointIndexes = new ArrayList<>();
        for (int i = 0; i < pAccelerationDataList.size() - 1; i++) {
            if((pAccelerationDataList.get(i + 1).getTime() - pAccelerationDataList.get(i).getTime()) < 400){
                if(pAccelerationDataList.get(i + 1).getValue() < pAccelerationDataList.get(i).getValue()){
                    wrongHighPointIndexes.add(i + 1);
                } else {
                    wrongHighPointIndexes.add(i);
                }
            }
        }
        for (int i = wrongHighPointIndexes.size() - 1; i >= 0; i--) {
            System.out.println(i);
            pAccelerationDataList.remove(i);
        }
        return pAccelerationDataList;
    }

    /**
     *
     * The method examineStepTypeAndSendResponse checks the overall acceleration of the highest peaks
     * pAccelerationData and sends all recognized steps via the registered interface stepListener.
     * If the total acceleration is greater than RUNNINGPEAK, the step type RUNNING is output,
     * if the total acceleration is greater than JOGGINGPEAK, the JOGGING step type is output,
     * otherwise the step type WALKING.
     * @param pAccelerationDataList An ArrayList with the highest high points
     */
    private void examineStepTypeAndSendResponse(ArrayList<AccelerationData> pAccelerationDataList){
        for (int i = 0; i < pAccelerationDataList.size(); i++) {
            AccelerationData highPoint = pAccelerationDataList.get(i);
            if(highPoint.getValue() > RUNNINGTHRESHOLD){
                stepListener.step(highPoint, StepType.RUNNING);
            } else if(highPoint.getValue() > JOGGINGTHRESHOLD){
                stepListener.step(highPoint, StepType.JOGGING);
            } else {
                stepListener.step(highPoint, StepType.WALKING);
            }
        }
    }

    /**
     * The DataSorter class is a comparator class for array lists with AccelerationData.
     * It is sorted upwards according to the size of the total acceleration value.
     */
    public class AccelerationDataSorter implements Comparator<AccelerationData> {
        /**
         * The compare method compares two data records from AccelerationData based on the
         * Total acceleration "value". If the acceleration of the first data set is greater,
         * 1 is returned. If the acceleration of the second data set is greater, -1 is returned.
         * Otherwise (if the acceleration is the same) 0 is returned.
         * @param data1 AccelerationData: The first comparison object
         * @param data2 AccelerationData: The second comparison object
         * @return int 0, 1 or -1; depending on which acceleration is greater.
         */
        @Override
        public int compare(AccelerationData data1, AccelerationData data2) {
            int returnValue = 0;
            if(data1.getValue() < data2.getValue()){
                returnValue = -1;
            } else if(data1.getValue() > data2.getValue()){
                returnValue = 1;
            }
            return returnValue;
        }
    }

}

