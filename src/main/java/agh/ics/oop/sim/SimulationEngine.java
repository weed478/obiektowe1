package agh.ics.oop.sim;

import agh.ics.oop.core.Vector2d;
import agh.ics.oop.map.IAnimalAndGrassMap;
import agh.ics.oop.objects.Animal;
import agh.ics.oop.objects.Grass;

import java.util.*;

public class SimulationEngine implements Runnable {

    private final IAnimalAndGrassMap map;
    private final List<Animal> animals;
    private int simulationDelay;
    private final Set<ISimulationStateObserver> observers = new HashSet<>();
    private boolean isRunning = true;
    private int averageDeadLifetimeSum = 0;
    private int allDeadAnimalsCount = 0;
    private int day = 0;

    public SimulationEngine(int simulationDelay, IAnimalAndGrassMap map, List<Animal> animals) {
        this.simulationDelay = simulationDelay;
        this.map = map;
        this.animals = animals;
    }

    public void addSimulationStateObserver(ISimulationStateObserver observer) {
        observers.add(observer);
    }

    public void removeSimulationStateObserver(ISimulationStateObserver observer) {
        observers.remove(observer);
    }

    public synchronized void setSimulationDelay(int simulationDelay) {
        this.simulationDelay = simulationDelay;
    }

    public synchronized void pause() {
        isRunning = false;
    }

    public synchronized void resume() {
        isRunning = true;
        notifyAll();
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    private void simulateDay() {
        removeDeadAnimals();
        moveAnimals();
        processEating();
        processReproduction();
        growPlants();
        passDay();
    }

    private void removeDeadAnimals() {
        Iterator<Animal> i = animals.iterator();
        while (i.hasNext()) {
            Animal animal = i.next();
            if (animal.isAlive()) continue;
            animal.elementRemoved();
            i.remove();
            averageDeadLifetimeSum += animal.getAge();
            allDeadAnimalsCount++;
        }
    }

    private void moveAnimals() {
        for (Animal animal : animals) {
            int turn = animal.decideMovement();
            animal.move(turn);
        }
    }

    private void processEating() {
        for (Animal animal : animals) {
            Vector2d pos = animal.getPosition();
            Grass grass = map.getGrassAt(pos);
            if (grass != null) {
                animal = map.getAnimalsAt(pos).get(0);
                animal.eatGrass(grass);
            }
        }
    }

    private void processReproduction() {
        Set<Vector2d> processedPositions = new HashSet<>();
        List<Animal> newAnimals = new ArrayList<>();
        for (Animal animal : animals) {
            Vector2d pos = animal.getPosition();
            if (processedPositions.contains(pos)) continue;
            processedPositions.add(pos);

            List<Animal> parents = map.getAnimalsAt(pos);
            if (parents.size() < 2 || !parents.get(0).canBreed() || !parents.get(1).canBreed())
                continue;
            Animal child = parents.get(0).breed(parents.get(1));
            newAnimals.add(child);
        }
        animals.addAll(newAnimals);
    }

    private void growPlants() {
        map.growGrass();
    }

    private void passDay() {
        for (Animal animal : animals) {
            animal.passDay();
        }
        day++;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                int delay;
                synchronized (this) {
                    while (!isRunning) wait();
                    delay = simulationDelay;
                }
                Thread.sleep(delay);
                synchronized (map) {
                    simulateDay();
                }
                simulationStateChanged();
            } catch (InterruptedException e) {
                break;
            }
        }
        simulationEnded();
    }

    public SimulationStatistics getStatistics() {
        double averageFood = 0;
        double averageChildren = 0;

        for (Animal a : animals) {
            averageFood += a.getFood();
            averageChildren += a.getNumChildren();
        }

        averageFood /= animals.size();
        averageChildren /= animals.size();

        return new SimulationStatistics(
                day,
                map.getAnimalCount(),
                map.getGrassCount(),
                averageFood,
                allDeadAnimalsCount == 0 ? 0 : ((double) averageDeadLifetimeSum / allDeadAnimalsCount),
                averageChildren
        );
    }

    private void simulationStateChanged() {
        for (ISimulationStateObserver observer : observers) {
            observer.simulationStateChanged();
        }
    }

    private void simulationEnded() {
        for (ISimulationStateObserver observer : observers) {
            observer.simulationEnded();
        }
    }
}
