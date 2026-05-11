package proj;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class MemoryManager implements Runnable {

    private static final int MAX_MEMORY = 2048;

    private Queue<PCB> jobQueue;
    private Queue<PCB> readyQueue;
    private List<PCB> watingQueue = new ArrayList<>();
    private int usedMemory = 0;
    private boolean fileReaderDone = false;
    private boolean schedulingDone = false; 

    public MemoryManager(Queue<PCB> jobQueue, Queue<PCB> readyQueue) {
        this.jobQueue = jobQueue;
        this.readyQueue = readyQueue;
    }

    public void setFileReaderDone() {
        synchronized (jobQueue) {
            fileReaderDone = true;
            jobQueue.notifyAll();
        }
    }

    
    public void setSchedulingDone() {
        synchronized (this) {
            schedulingDone = true;
            this.notifyAll();
        }
    }

    @Override
    public void run() {

        System.out.println("Thread 2 started: Memory Manager is running...\n");

        // ── Phase 1: initial job queue loading ───────────────────────────────
        while (true) {

            PCB process = null;

            synchronized (jobQueue) {
                while (jobQueue.isEmpty() && !fileReaderDone) {
                    try {
                        jobQueue.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                if (jobQueue.isEmpty() && fileReaderDone) {
                    break;
                }

                process = jobQueue.poll();
            }

            if (process == null)
                continue;

            if (usedMemory + process.getMemoryRequired() <= MAX_MEMORY) {

                usedMemory += process.getMemoryRequired();
                process.setState(Pstate.READY);

                synchronized (readyQueue) {
                    readyQueue.add(process);
                }

                System.out.println("Admitted: P" + process.getProcessId()
                        + " | Memory Used: " + usedMemory + "/" + MAX_MEMORY + " MB");

            } else {
                System.out.println("Waiting: P" + process.getProcessId()
                        + " | Needs: " + process.getMemoryRequired()
                        + " MB | Available: " + (MAX_MEMORY - usedMemory) + " MB");
                watingQueue.add(process);
            }
        }

        // ── Phase 2: stay alive until scheduling finishes ────────────────────
        synchronized (this) {
            while (!schedulingDone) {
                try {
                    this.wait(); // woken up by freeMemory()
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                // when woken up retry waiting processes
                retryRejected();
            }
        }

        System.out.println("\nThread 2 finished.");
    }

    public void freeMemory(PCB process) {
        usedMemory -= process.getMemoryRequired();
        if (usedMemory < 0)
            usedMemory = 0;
        process.setState(Pstate.TERMINATED);
        System.out.println("Freed: P" + process.getProcessId()
                + " | Memory Used: " + usedMemory + "/" + MAX_MEMORY + " MB");

        
        synchronized (this) {
            this.notifyAll();
        }
    }

    public void freeMemoryAll(List<PCB> processes) {
        System.out.println("\nFreeing memory after scheduling:");
        for (PCB p : processes) {
            freeMemory(p);
        }
    }

    public void retryRejected() {
        List<PCB> retry = new ArrayList<>(watingQueue);
        watingQueue.clear();
        for (PCB process : retry) {
            if (usedMemory + process.getMemoryRequired() <= MAX_MEMORY) {
                usedMemory += process.getMemoryRequired();
                process.setState(Pstate.READY);
                synchronized (readyQueue) {
                    readyQueue.add(process);
                }
                System.out.println("Admitted: P" + process.getProcessId()
                        + " | Memory Used: " + usedMemory + "/" + MAX_MEMORY + " MB");
            } else {
                watingQueue.add(process); // still can't fit, keep waiting
            }
        }
    }
}