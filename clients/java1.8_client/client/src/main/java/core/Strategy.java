package core;

import core.API.Elevator;
import core.API.Passenger;

import java.util.List;

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




    public void onTick(List<Passenger> myPassengers, List<Elevator> myElevators, List<Passenger> enemyPassengers,
                       List<Elevator> enemyElevators) {
        for (Elevator e : myElevators) {
            for (Passenger p : myPassengers) {
                if (p.getState() < P_STATE_USING_ELEVATOR) {
                    if (e.getState() != E_STATE_MOVING) {
                        e.goToFloor(p.getFromFloor());
                    }
                    if (e.getFloor() == p.getFromFloor()) {
                        p.setElevator(e);
                    }
                }
            }
            if (e.getPassengers().size() > 0 && e.getState() != E_STATE_MOVING) {
                e.goToFloor(e.getPassengers().get(0).getDestFloor());
            }
        }
    }
}
