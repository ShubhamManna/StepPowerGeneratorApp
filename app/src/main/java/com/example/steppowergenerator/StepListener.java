package com.example.steppowergenerator;

/**
 * The StepListener interface is used to identify steps recognized by the StepDetector class
 * back to the activity or the class in which the interface is implemented, to "send" back.
 * To do this, the class in which the interface is implemented must be registered in the StepDetector.
 */
public interface StepListener {

    /**
     * The step method should transfer recognized steps.
     * @param accelerationData AccelerationData: A data record of the acceleration sensor, which stands for a step.
     * @param stepType Enum StepType: One of the three step types from the Enum StepType.
     */
    void step(AccelerationData accelerationData, StepType stepType);
}
