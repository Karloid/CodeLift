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
    public static final int END = 7200;
    private List<Passenger> myPassengers;
    private List<Elevator> myElevators;
    private List<Passenger> enemyPassengers;
    private List<Elevator> enemyElevators;
    private List<Passenger> allPass;
    private int tick;
    private int ticksToEnd;
    private boolean noMorePickUps;


    public void onTick(List<Passenger> myPassengers, List<Elevator> myElevators, List<Passenger> enemyPassengers,
                       List<Elevator> enemyElevators) {
        tick++;
        ticksToEnd = END - tick;

        noMorePickUps = ticksToEnd < 650;


        if (tick == 1) {
            setDebug(new Debug());
        }
        this.myPassengers = myPassengers;
        this.allPass = new ArrayList<>(myPassengers);
        this.allPass.addAll(enemyPassengers);
        this.myElevators = myElevators;
        this.enemyPassengers = enemyPassengers;
        this.enemyElevators = enemyElevators;

        try {
            doMove();
            if (tick == END) {
                print("end");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void doMove() {
        for (Passenger p : allPass) {
            if (p.getState() >= P_STATE_USING_ELEVATOR) {
                continue;
            }

            List<Elevator> candidates = new ArrayList<>();
            for (Elevator e : myElevators) {
                if (e.getPassengers().size() < 20
                        && Objects.equals(e.getFloor(), p.getFloor())) {
                    candidates.add(e);
                }
            }

            if (!candidates.isEmpty()) {
                Elevator min;
                min = Collections.min(candidates, Comparator.comparingDouble(o -> getDistance(p, o)));
              /*  if (tick > 40) {  //TODO detect distance to enemy elevators and send passengers to most far my elevator
                    min = Collections.min(candidates, Comparator.comparingDouble(o -> getDistance(p, o)));
                } else {
                    min = Collections.max(candidates, Comparator.comparingDouble(o -> getDistance(p, o)));
                }*/

                //TODO consider group passengers
                setElevatorToPass(p, min);

            }
        }

        for (Elevator e : myElevators) {
            Integer eFloor = e.getFloor();

            if (elevatorMustGo(e)) {
                Passenger nearPassenger = null;
                List<Passenger> passengers = e.getPassengers();
                if (passengers == null) {
                    passengers = new ArrayList<>();
                } else {
                    passengers = new ArrayList<>(passengers);
                }


                int direction = 0;

                for (Passenger passenger : passengers) {
                    int points = getPoints(passenger);
                    Integer destFloor = passenger.getDestFloor();
                    if (destFloor > eFloor) {
                        direction += points;
                    } else if (destFloor < eFloor) {
                        direction -= points;
                    } else {
                        print(e.getId() + " strange passenger, same floor " + destFloor);
                    }
                }

                if (direction == 0) {
                    direction = -1;
                }

                int finalDirection = direction;
                passengers.removeIf(passenger -> {
                    if (finalDirection < 0) {
                        return passenger.getDestFloor() > eFloor;
                    } else {
                        return passenger.getDestFloor() < eFloor;
                    }
                });


                if (!passengers.isEmpty()) {
                    nearPassenger = Collections.min(passengers, Comparator.comparing(o -> Math.abs(o.getDestFloor() - e.getFloor())));
                    Passenger buzyPass = null;
                    if (noMorePickUps) {
                        List<Map.Entry<Passenger, Integer>> passFloors = new ArrayList<>();

                        for (Passenger passenger : e.getPassengers()) {
                            int points = getPoints(passenger);

                            //TODO koeff for moving downward

                            boolean shouldInsert = true;
                            for (Map.Entry<Passenger, Integer> passFloor : passFloors) {
                                if (Objects.equals(passFloor.getKey().getDestFloor(), passenger.getDestFloor())) {
                                    passFloor.setValue(passFloor.getValue() + points);
                                    shouldInsert = false;
                                }
                            }

                            if (shouldInsert) {
                                passFloors.add(new AbstractMap.SimpleEntry<>(passenger, points));
                            }
                        }

                        //TODO pick most points
                        print("--- elevator " + e.getId() + " -- passFloors:  UNSORTED vvv");

                        printPassFloors(passFloors);

                        passFloors.sort(Comparator.comparing(Map.Entry::getValue));

                        print("--- elevator " + e.getId() + " -- passFloors:  SORTED");
                        printPassFloors(passFloors);

                        print("--- elevator " + e.getId() + " -- passFloors:  END ^^^ ");

                        buzyPass = passFloors.get(passFloors.size() - 1).getKey();

                        if (!Objects.equals(buzyPass.getDestFloor(), nearPassenger.getDestFloor())) {
                            print("Nearest and busiest floors not equal ! busy: " + buzyPass.getDestFloor() + " nearest:  " + nearPassenger.getDestFloor());
                        }
                        nearPassenger = buzyPass;
                    }
                }

                Set<Integer> floorsWithP = new HashSet<>();
                for (Passenger p : allPass) {
                    if (p.getState() == E_STATE_WAITING) {
                        //TODO look at other elevators
                        floorsWithP.add(p.getFloor());
                    }
                }

                for (Elevator elevator : myElevators) {
                    if (elevator.getNextFloor() != null) {
                        floorsWithP.remove(elevator.getNextFloor());
                    }
                }


                if (nearPassenger != null) {

                    Integer pDestFloor = nearPassenger.getDestFloor();

                    //TODO remove floors which not on the way
                    if (pDestFloor > eFloor) {
                        floorsWithP.removeIf(integer -> integer > pDestFloor || integer < eFloor);
                    } else {
                        floorsWithP.removeIf(integer -> integer > eFloor || integer < pDestFloor);
                    }

                    Integer nearCrowdFloor = getNearestFloor(e, floorsWithP);

                    if (!noMorePickUps && nearCrowdFloor != null && e.getPassengers().size() < 20
                            && getDistance(nearCrowdFloor, e.getFloor()) < getDistance(pDestFloor, e.getFloor())) {
                        print(e.getId() + " going to intermediate floor " + nearCrowdFloor);
                        goToFloor(e, nearCrowdFloor);
                    } else {
                        goToFloor(e, pDestFloor);
                    }

                } else {

                    Integer nearCrowdFloor = getNearestFloor(e, floorsWithP);

                    if (nearCrowdFloor != null) {
                        goToFloor(e, nearCrowdFloor);
                    } else if (eFloor == 1) {
                        goToFloor(e, 2);
                    } else {
                        goToFloor(e, eFloor - 1);
                    }
                }
            }
        }
    }

    private void printPassFloors(List<Map.Entry<Passenger, Integer>> passFloors) {
        for (Map.Entry<Passenger, Integer> passFloor : passFloors) {
            print("passFloor: " + passFloor.getKey().getDestFloor() + " points: " + passFloor.getValue());
        }
    }

    private int getPoints(Passenger passenger) {
        return myPassengers.contains(passenger) ? 1 : 2;
    }

    private Integer getNearestFloor(Elevator e, Set<Integer> floorsWithP) {
        Integer nearCrowdFloor = null;
        if (!floorsWithP.isEmpty()) {
            nearCrowdFloor = Collections.min(floorsWithP, Comparator.comparing(o -> Math.abs(o - e.getFloor())));
        }
        return nearCrowdFloor;
    }

    private int getDistance(Integer x, Integer y) {
        return Math.abs(x - y);
    }

    private double getDistance(Passenger p, Elevator o) {
        return Math.abs(getElX(o) + 300 - (p.getX() + 300));
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
        if (e.getPassengers().size() == 20) {
            return true;
        }

        if (!noMorePickUps && nobodyOnFloor(e.getFloor())) {
            return true;
        }
       /* if (e.getTimeOnFloor() > 200) {
            return true;
        }*/

        return false;
    }

    private boolean nobodyOnFloor(Integer floor) {
        for (Passenger pass : allPass) {
            if (Objects.equals(pass.getFloor(), floor) && Objects.equals(pass.getFromFloor(), floor)
                    && (pass.getState() == P_STATE_WAITING_ELEVATOR ||
                    (pass.getState() == P_STATE_MOVING_TO_ELEVATOR && isMyElevator(pass.getElevator()))
            )) {
                return false;
            }
        }
        return true;
    }

    private boolean isMyElevator(Integer elevator) {
        if (elevator == null) {
            return false;
        }
        for (Elevator myElevator : myElevators) {
            if (myElevator.getId().equals(elevator)) {
                return true;
            }
        }
        return false;
    }

    private int getElX(Elevator o1) {
        switch (o1.getId()) {
            case 7:
                return -300;
            case 5:
                return -220;
            case 3:
                return -140;
            case 1:
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
