package core;

import core.API.Debug;
import core.API.Elevator;
import core.API.Passenger;

import java.util.*;

public class Strategy extends BaseStrategy {

    public static final float SPEED_DOWNWARD = 1 / 50f;

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
    public static final int MIN_POINTS = 50;
    public static boolean printEnabled = false;
    private List<Passenger> myPassengers;
    private List<Elevator> myElevators;
    private List<Passenger> enemyPassengers;
    private List<Elevator> enemyElevators;
    private List<Passenger> allPass;
    private int tick;
    private int ticksToEnd;
    private boolean noMorePickUps;
    private Map<Integer, FloorPotential> floorPotential;
    private ArrayList<FloorPotential> potentialFloorsByTotalPoints;

    private long totalMs;


    public void onTick(List<Passenger> myPassengers, List<Elevator> myElevators, List<Passenger> enemyPassengers,
                       List<Elevator> enemyElevators) {
        long start = System.currentTimeMillis();
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
        } catch (Throwable e) {
            e.printStackTrace();
        }

        totalMs += System.currentTimeMillis() - start;

        if (tick == END) {
            print("end, timeElapsed " + totalMs + "ms, mean time per tick " + (totalMs / 7200f) + "ms");
        }
    }

    private void doMove() {
        calcFloorPotential();


        for (Passenger p : allPass) {
            if (p.getState() >= P_STATE_MOVING_TO_FLOOR) {
                continue;
            }

            boolean isPoorPass = isPoorPass(p);


            List<Elevator> candidates = new ArrayList<>();
            for (Elevator e : myElevators) {
                if (notFullOnFloor(e, p.getFloor())) {
                    if (elevatorCanPickUp(p, isPoorPass, e)) {
                        candidates.add(e);
                    }
                }
            }

            if (!candidates.isEmpty()) {
                Elevator result;
                // different logic for first floor and game start
                if (tick < 2300 && p.getFloor() == 1) {
                    //trying to group passengers by floors
                    candidates.sort(Comparator.comparing(Elevator::getId));
                    int size = candidates.size();
                    float scale = 9 / (size * 1.f);

                    int v = size - 1 - Math.min((int) (p.getDestFloor() / scale), size - 1);

                    result = candidates.get(v);


                } else {
                    List<Elevator> safeElev = getSafeElev(candidates, p);

                    if (!safeElev.isEmpty()) {
                        Integer destFloor = p.getDestFloor();

                        List<Map.Entry<Elevator, Integer>> elevatorWithFloor = new ArrayList<>();
                        for (Elevator elevator : safeElev) {
                            AbstractMap.SimpleEntry<Elevator, Integer> entry = new AbstractMap.SimpleEntry<>(elevator, 0);

                            for (Passenger pass : allPass) {
                                if (pass.getState() != P_STATE_EXITING
                                        && pass.getState() != P_STATE_EXITING
                                        && pass.getState() != P_STATE_RETURNING
                                        && pass.getState() != P_STATE_MOVING_TO_FLOOR
                                        && Objects.equals(pass.getElevator(), elevator.getId())
                                        && Objects.equals(pass.getDestFloor(), destFloor)) {
                                    entry.setValue(entry.getValue() + 1);
                                }
                            }

                            elevatorWithFloor.add(entry);
                        }


                        result = Collections.max(elevatorWithFloor, Comparator.comparing(Map.Entry::getValue)).getKey();   //TODO group passengers
                    } else {
                        result = Collections.min(candidates, Comparator.comparingDouble(o -> getDistance(p, o)));
                    }

                }

                //TODO consider group passengers
                setElevatorToPass(p, result);

            }
        }

        for (Elevator e : myElevators) {
            //  print("Elevator speed " + e.getId() + " " + e.getSpeed());
            moveElevator(e);
        }
    }

    private boolean isPoorPass(Passenger p) {
        return getPoints(p) <= MIN_POINTS;
    }

    private boolean elevatorCanPickUp(Passenger p, Elevator e) {
        return elevatorCanPickUp(p, isPoorPass(p), e);
    }

    private boolean elevatorCanPickUp(Passenger p, boolean isPoorPass, Elevator e) {
        return !isPoorPass || elevatorHasPassengersWithFloor(e, p.getDestFloor());
    }

    private boolean elevatorHasPassengersWithFloor(Elevator e, Integer destFloor) {
        for (Passenger passenger : e.getPassengers()) {
            if (passenger.getDestFloor().equals(destFloor)) {
                return true;
            }
        }
        return false;
    }

    private void calcFloorPotential() {
        floorPotential = new HashMap<>();
        for (int i = 1; i <= 9; i++) {
            floorPotential.put(i, new FloorPotential(i));
        }

        for (Passenger p : allPass) {
            if (p.getState() >= P_STATE_MOVING_TO_FLOOR) {
                continue;
            }

            floorPotential.get(p.getFloor()).addP(p);
        }


        potentialFloorsByTotalPoints = new ArrayList<>(floorPotential.values());

        potentialFloorsByTotalPoints.sort(Comparator.comparing(fp -> {
            return fp.pointsTotal;
            //return Collections.max(fp.destPotential.values()); //results is equal to 3904
        }, Collections.reverseOrder()));
        print("By pointsTotal");
        printPotentials(potentialFloorsByTotalPoints);

         /*
        asList.sort(Comparator.comparing(fp -> fp.pointsDown, Collections.reverseOrder()));
        print("By pointsDown");
        printPotentials(asList);
        asList.sort(Comparator.comparing(fp -> fp.pointsUp, Collections.reverseOrder()));
        print("By pointsUp");
        printPotentials(asList);*/
    }

    private void printPotentials(ArrayList<FloorPotential> asList) {
        print("---vvv---");
        for (FloorPotential potential : asList) {
            print(potential.toString());
        }
        print("---^^^---");
        print("");
    }

    private void moveElevator(Elevator e) {
        if (!elevatorMustGo(e)) {
            return;
        }

        Integer eFloor = e.getFloor();
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
            if (destFloor > eFloor) {   //TODO uniform distribution across floors
                direction += points;
            } else if (destFloor < eFloor) {
                direction -= points * 1; // downward is preferable
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
            //  if (noMorePickUps) { //looking for most profit floor
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
            //  print("--- elevator " + e.getId() + " -- passFloors:  UNSORTED vvv");

            printPassFloors(passFloors);

            passFloors.sort(Comparator.comparing(Map.Entry::getValue));

            //   print("--- elevator " + e.getId() + " -- passFloors:  SORTED");
            printPassFloors(passFloors);

            //     print("--- elevator " + e.getId() + " -- passFloors:  END ^^^ ");

            buzyPass = passFloors.get(passFloors.size() - 1).getKey();

            if (!Objects.equals(buzyPass.getDestFloor(), nearPassenger.getDestFloor())) {
                print("Nearest and busiest floors not equal ! busy: " + buzyPass.getDestFloor() + " nearest:  " + nearPassenger.getDestFloor());
            }
            nearPassenger = buzyPass;
            //      }
        }

        Set<Integer> floorsWithP = new HashSet<>();
        for (Passenger p : allPass) {
            if ((p.getState() == P_STATE_WAITING_ELEVATOR || p.getState() == P_STATE_RETURNING) &&
                    elevatorCanPickUp(p, e) && passWillStayUntilElevatorCome(p, e)) {
                floorsWithP.add(p.getFloor());
            }
        }

        for (Elevator elevator : myElevators) {
            if (elevator.getNextFloor() != null) {
                floorsWithP.remove(elevator.getNextFloor());
                if (elevator.getState() != E_STATE_MOVING && elevator.getState() != E_STATE_CLOSING) {
                    floorsWithP.remove(elevator.getFloor());
                }
            }
        }


        if (nearPassenger != null) {

            Integer pDestFloor = nearPassenger.getDestFloor();
            //TODO remove floors if they didn't have passengers for way

            if (pDestFloor > eFloor) {
                floorsWithP.removeIf(integer -> integer > pDestFloor || integer < eFloor);
            } else {
                floorsWithP.removeIf(integer -> integer > eFloor || integer < pDestFloor);
            }

            Integer nearCrowdFloor = getNearestFloor(e, floorsWithP);

            if (!noMorePickUps && nearCrowdFloor != null && e.getPassengers().size() < 20   //TODO remove this condition
                    && getDistance(nearCrowdFloor, e.getFloor()) < getDistance(pDestFloor, e.getFloor())) {
                print(e.getId() + " going to intermediate floor " + nearCrowdFloor);
                goToFloor(e, nearCrowdFloor);
            } else {
                goToFloor(e, pDestFloor);
            }

        } else {

            Integer targetFloor = null;
            for (FloorPotential fp : potentialFloorsByTotalPoints) {
                if (floorsWithP.contains(fp.floor)) {
                    targetFloor = fp.floor;
                    break;
                }
            }

            if (targetFloor != null) {
                goToFloor(e, targetFloor);
            } else if (eFloor == 1) {
                goToFloor(e, 2);
            } else {
                goToFloor(e, eFloor - 1);
            }
        }
    }

    private boolean passWillStayUntilElevatorCome(Passenger p, Elevator e) {
        Double speed = p.getY() > e.getY() ? e.getSpeed() : SPEED_DOWNWARD;
        double ticksToCome = Math.abs(p.getY() - e.getY()) / speed;

        return p.getTimeToAway() > ticksToCome + (isMyPass(p) ? 0 : 140);
    }

    private boolean isMyPass(Passenger p) {
        return myPassengers.contains(p);
    }

    private boolean notFullOnFloor(Elevator e, Integer floor) {
        return e.getPassengers().size() < 20
                && Objects.equals(e.getFloor(), floor);
    }

    private List<Elevator> getSafeElev(List<Elevator> candidates, Passenger p) {
        boolean isEnemyP = enemyPassengers.contains(p);
        ArrayList<Elevator> best = new ArrayList<>(candidates);
        for (Elevator enemyElevator : enemyElevators) {
            boolean isCanCall = isEnemyP || enemyElevator.getTimeOnFloor() > 140 || (tick < 2000 && p.getFloor() == 1);
            if (isCanCall && notFullOnFloor(enemyElevator, p.getFloor())) {
                best.removeIf(elevator -> getDistance(p, elevator) > getDistance(p, enemyElevator));
            }
        }

        return best;
    }

    private void printPassFloors(List<Map.Entry<Passenger, Integer>> passFloors) {
        for (Map.Entry<Passenger, Integer> passFloor : passFloors) {
            print("passFloor: " + passFloor.getKey().getDestFloor() + " points: " + passFloor.getValue());
        }
    }

    private int getPoints(Passenger passenger) {
        boolean isMy = myPassengers.contains(passenger);
        int points = Math.abs(passenger.getDestFloor() - passenger.getFromFloor()) * 10;

        return points * (isMy ? 1 : 2);
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
        if (printEnabled) {
            System.out.println(tick + ": " + msg);
            log(msg);
        }
    }

    private boolean elevatorMustGo(Elevator e) {

        if (e.getState() == E_STATE_MOVING || e.getState() == E_STATE_OPENING /*|| e.getState() == E_STATE_CLOSING*/) {
            return false;
        }
        if (e.getTimeOnFloor() < 100) {
            return false;
        }

        int passengersCount = e.getPassengers().size();
        if (noMorePickUps && passengersCount > 0) {
            return true;
        }

        if (passengersCount == 20) {
            return true;
        }

        if (nobodyOnElevatorFloor(e)) {

            if (tick > 2000 || !e.getFloor().equals(1)) {
                return true;
            }
        }

        return false;
    }

    private boolean nobodyOnElevatorFloor(Elevator e) { //TODO check points
        for (Passenger pass : allPass) {
            if (Objects.equals(pass.getFloor(), e.getFloor()) && Objects.equals(pass.getFromFloor(), e.getFloor())
                    && (pass.getState() == P_STATE_WAITING_ELEVATOR ||/* pass.getState() == P_STATE_RETURNING ||*/
                    //  (pass.getState() == P_STATE_MOVING_TO_ELEVATOR && pass.getElevator().equals(e.getId()))
                    pass.getState() == P_STATE_RETURNING ||
                    (pass.getState() == P_STATE_MOVING_TO_ELEVATOR && isMyElevator(pass.getElevator()))
            ) && elevatorCanPickUp(pass, e)) {
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

    private class FloorPotential {
        private int floor;
        private int pointsTotal;
        private int pointsUp;
        private int pointsDown;
        private HashMap<Integer, Integer> destPotential;

        public FloorPotential(int floor) {

            this.floor = floor;
            destPotential = new HashMap<>();
            for (int i = 1; i <= 9; i++) {
                destPotential.put(i, 0);
            }
        }

        public void addP(Passenger p) {
            int points = getPoints(p);

            pointsTotal = pointsTotal + points;
            pointsUp += p.getDestFloor() > floor ? points : 0;
            pointsDown += p.getDestFloor() < floor ? points : 0;

            destPotential.put(p.getDestFloor(), destPotential.get(p.getDestFloor()) + points);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("FloorPotential{");
            sb.append("floor=").append(floor);
            sb.append(", pointsTotal=").append(pointsTotal);
            sb.append(", pointsUp=").append(pointsUp);
            sb.append(", pointsDown=").append(pointsDown);
            sb.append(", destPotential=").append(destPotential);
            sb.append('}');
            return sb.toString();
        }
    }
}
