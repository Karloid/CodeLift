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
        if (tick == 7200) {
            print("end");
        }
    }

    private void doMove() {
        for (Passenger p : allPass) {
            if (p.hasElevator() || p.getState() >= P_STATE_USING_ELEVATOR) {
                continue;
            }

            List<Elevator> candidates = new ArrayList<>();
            for (Elevator e : myElevators) {
                if (e.getPassengers().size() < 20
                        && Objects.equals(e.getFloor(), p.getFromFloor())
                        && (myPassengers.contains(p)
                        || e.getTimeOnFloor() > 40)) {
                    candidates.add(e);
                }
            }

            if (!candidates.isEmpty()) {
                Elevator min = Collections.min(candidates, Comparator.comparingDouble(o -> getDistance(p, o)));

                setElevatorToPass(p, min);
            }
        }

        for (Elevator e : myElevators) {
            if (elevatorMustGo(e)) {
                Passenger min = null;
                if (e.getPassengers() != null && !e.getPassengers().isEmpty()) {
                    min = Collections.min(e.getPassengers(), Comparator.comparing(o -> Math.abs(o.getDestFloor() - e.getFloor())));
                }

                if (min != null) {
                    goToFloor(e, min.getDestFloor());
                } else {
                    Integer floor = e.getFloor();
                    if (floor == 1) {
                        goToFloor(e, 2);
                    } else {
                        goToFloor(e, floor - 1);
                    }
                }
            }
        }
    }

    private double getDistance(Passenger p, Elevator o) {
        return Math.abs(getElX(o) - p.getX());
    }

    private void setElevatorToPass(Passenger p, Elevator e) {
        print(" pass: " + (myPassengers.contains(p) ? "MY" : "ENEMY") + " " + p.getId() +
                " set elevator " + e.getId() + " dist: " + getDistance(p, e)
                + " elevator state " + e.getState() + " timeOnFloor "
                + e.getTimeOnFloor() + " elevat must go " + elevatorMustGo(e));
        p.setElevator(e);
    }

    private void goToFloor(Elevator e, Integer destFloor) {
        String msg = "Lift: " + e.getId() + " go to floor " + destFloor + " people inside " + e.getPassengers().size();
        print(msg);
        e.goToFloor(destFloor);
    }

    private void print(String msg) {
        System.out.println(tick + ": " + msg);
        log(msg);
    }

    private boolean elevatorMustGo(Elevator e) {

        if (e.getState() == E_STATE_MOVING) {
            return false;
        }
        if (e.getTimeOnFloor() < 40) {
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
