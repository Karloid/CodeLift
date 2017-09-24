package core;

import core.API.Debug;
import core.API.Elevator;
import core.API.Passenger;

import java.util.*;

public class Strategy extends BaseStrategy {

    public static final int E_STATE_WAITING = 0;
    public static final int E_STATE_MOVING = 1;
    public static final int E_STATE_OPENING = 2;
    public static final int E_STATE_FILLING = 3;
    public static final int E_STATE_CLOSING = 4;


    public static final int P_STATE_WAITING_ELEVATOR = 1;
    public static final int P_STATE_MOVING_TO_ELEVATOR = 2;
    public static final int P_STATE_RETURNING = 3;
    public static final int P_STATE_MOVING_TO_FLOOR = 4;
    public static final int P_STATE_USING_ELEVATOR = 5;
    public static final int P_STATE_EXITING = 6;
    private List<Passenger> myPassengers;
    private List<Elevator> myElevators;
    private List<Passenger> enemyPassengers;
    private List<Elevator> enemyElevators;
    private List<Passenger> allPass;
    private int tick;


    public void onTick(List<Passenger> myPassengers, List<Elevator> myElevators, List<Passenger> enemyPassengers,
                       List<Elevator> enemyElevators) {
        tick++;
        if (tick == 1) {
            setDebug(new Debug());
        }
        this.myPassengers = myPassengers;
        this.allPass = new ArrayList<>(myPassengers);
        this.allPass.addAll(enemyPassengers);
        this.myElevators = myElevators;
        this.enemyPassengers = enemyPassengers;
        this.enemyElevators = enemyElevators;


        doMove();
    }

    private void doMove() {
        for (Passenger p : allPass) {
            if (p.hasElevator() || p.getState() >= P_STATE_USING_ELEVATOR) {
                continue;
            }

            List<Elevator> candidates = new ArrayList<>();
            for (Elevator e : myElevators) {
                if (Objects.equals(e.getFloor(), p.getFromFloor()) && (myPassengers.contains(p) || e.getTimeOnFloor() > 40)) {
                    candidates.add(e);
                }
            }

            Elevator min = Collections.min(candidates, Comparator.comparingDouble(o -> Math.abs(getElX(o) - p.getX())));

            if (min != null) {
                p.setElevator(min);
            }
        }

        for (Elevator e : myElevators) {
            if (elevatorMustGo(e)) {
                Passenger min = Collections.min(e.getPassengers(), Comparator.comparing(o -> Math.abs(o.getDestFloor() - e.getFloor())));

                if (min != null) {
                    e.goToFloor(min.getDestFloor());
                } else {
                    Integer floor = e.getFloor();
                    if (floor == 1) {
                        e.goToFloor(2);
                    } else {
                        e.goToFloor(floor - 1);
                    }
                }
            }
        }
    }

    private boolean elevatorMustGo(Elevator e) {
        if (e.getState() == E_STATE_MOVING) {
            return false;
        }
        if (e.getPassengers().size() > 5) {
            return true;
        }

        if (nobodyOnFloor(e.getFloor())) {
            return true;
        }
        if (e.getTimeOnFloor() > 200) {
            return true;
        }

        return false;
    }

    private boolean nobodyOnFloor(Integer floor) {
        for (Passenger pass : allPass) {
            if (Objects.equals(pass.getFloor(), floor) && Objects.equals(pass.getFromFloor(), floor)) {
                return false;
            }
        }
        return true;
    }

    private int getElX(Elevator o1) {
        switch (o1.getId()) {
            case 1:
                return -300;
            case 3:
                return -220;
            case 5:
                return -140;
            case 7:
                return -60;
            case 2:
                return 60;
            case 4:
                return 140;
            case 6:
                return 220;
            case 8:
                return 300;
        }
        return 0;
    }

}
