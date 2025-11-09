package net.mooctest;

import java.util.List;

public class PredictiveSchedulingStrategy implements DispatchStrategy {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, PassengerRequest request) {
        // 预测需求，并选择最适合的电梯
        Elevator bestElevator = null;
        double lowestPredictedCost = Double.MAX_VALUE;

        for (Elevator elevator : elevators) {
            double predictedCost = calculatePredictedCost(elevator, request);

            if (predictedCost < lowestPredictedCost) {
                lowestPredictedCost = predictedCost;
                bestElevator = elevator;
            }
        }

        return bestElevator;
    }

    public double calculatePredictedCost(Elevator elevator, PassengerRequest request) {
        int distance = Math.abs(elevator.getCurrentFloor() - request.getStartFloor());
        double loadFactor = elevator.getPassengerList().size() / elevator.getMaxLoad();

        return distance + loadFactor * 10; // 简化预测公式
    }
}
