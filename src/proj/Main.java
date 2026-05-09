package proj;

import java.util.LinkedList;
import java.util.Queue;

public class Main {

    public static void main(String[] args) {

        Queue<PCB> jobQueue = new LinkedList<>();
        Queue<PCB> readyQueue = new LinkedList<>();

        Thread fileReader = new Thread(
                new FileReaderThread("src/proj/job.txt", jobQueue)
        );

        fileReader.start();

        try {
            fileReader.join();
        } catch (InterruptedException e) {
            System.out.println("File Reader Thread interrupted.");
        }

        Thread memoryManager = new Thread(
                new MemoryManager(jobQueue, readyQueue)
        );

        memoryManager.start();

        try {
            memoryManager.join();
        } catch (InterruptedException e) {
            System.out.println("Memory Manager Thread interrupted.");
        }

        System.out.println("\nProcesses inside Ready Queue:");

        for (PCB p : readyQueue) {
            System.out.println(p);
        }
    }
}