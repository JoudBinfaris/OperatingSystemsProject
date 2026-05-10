package proj;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Queue<PCB> jobQueue = new LinkedList<>();
        Queue<PCB> readyQueue = new LinkedList<>();

        MemoryManager memoryManagerRunnable = new MemoryManager(jobQueue, readyQueue);

        Thread fileReader = new Thread(
                new FileReaderThread("src/proj/job.txt", jobQueue, memoryManagerRunnable));

        Thread memoryManager = new Thread(memoryManagerRunnable);

        fileReader.start();
        memoryManager.start();

        try {
            fileReader.join();
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted.");
        }

        System.out.println("\nProcesses inside Ready Queue:");
        for (PCB p : readyQueue) {
            System.out.println(p);
        }


        Scanner scanner = new Scanner(System.in);

        System.out.println("Choose a scheduling algorithm:");
        System.out.println("1. Shortest Job First (SJF)");
        System.out.println("2. Round Robin (q = 5 ms)");
        System.out.println("3. Priority Scheduling (with Aging)");
        System.out.print("Enter your choice (1-3): ");

        int choice = scanner.nextInt();

        switch (choice) {
            case 1:
                SJFScheduler sjf = new SJFScheduler(readyQueue, memoryManagerRunnable);
                sjf.run();
                break;

            case 2:
                RoundRobinScheduler rr = new RoundRobinScheduler(readyQueue, memoryManagerRunnable);
                rr.run();
                break;
            case 3:
                PriorityScheduler ps = new PriorityScheduler(readyQueue, memoryManagerRunnable);
                ps.run();
                break;

            default:
                System.out.println("Invalid choice. Please enter 1, 2, or 3.");
                break;
        }

        memoryManagerRunnable.setSchedulingDone();

        try {
            memoryManager.join();
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted.");
        }

        scanner.close();
    }
}