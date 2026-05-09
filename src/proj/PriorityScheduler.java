package proj;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PriorityScheduler {

    private static final int AGING_INTERVAL = 4;
    private static final int MIN_PRIORITY = 1;

    private List<PCB> processes;

    public PriorityScheduler(List<PCB> readyQueue) {
        this.processes = new ArrayList<>(readyQueue);
    }

    public void run() {

        System.out.println("\n==========================================");
        System.out.println("   Priority Scheduler (Non-Preemptive)   ");
        System.out.println("==========================================\n");

        int n = processes.size();
        List<PCB> waitingQueue = new ArrayList<>(processes);

        int[] startTime = new int[n];
        int[] terminationTime = new int[n];
        int[] waitingTime = new int[n];
        int[] turnaroundTime = new int[n];
        boolean[] starved = new boolean[n];
        int[] waitedSoFar = new int[n];

        StringBuilder gantt = new StringBuilder();
        StringBuilder ganttTimes = new StringBuilder();

        int currentTime = 0;
        int lastAgingTime = 0;

        while (!waitingQueue.isEmpty()) {

            while (currentTime - lastAgingTime >= AGING_INTERVAL) {
                lastAgingTime += AGING_INTERVAL;
                for (PCB p : waitingQueue) {
                    int newPriority = p.getPriority() - 1;
                    if (newPriority < MIN_PRIORITY) newPriority = MIN_PRIORITY;
                    p.setPriority(newPriority);
                }
            }

            int queueSize = waitingQueue.size();
            int starvationThreshold = queueSize * 5;

            for (PCB p : waitingQueue) {
                int idx = indexOf(p);
                if (waitedSoFar[idx] > starvationThreshold) {
                    starved[idx] = true;
                }
            }

            waitingQueue.sort(Comparator.comparingInt(PCB::getPriority));

            PCB p = waitingQueue.remove(0);
            int idx = indexOf(p);

            p.setState(Pstate.RUNNING);

            startTime[idx] = currentTime;
            terminationTime[idx] = currentTime + p.getBurstTime();
            waitingTime[idx] = currentTime - p.getArrivalTime();
            turnaroundTime[idx] = terminationTime[idx] - p.getArrivalTime();

            p.setWaitingTime(waitingTime[idx]);
            p.setTurnaroundTime(turnaroundTime[idx]);
            p.setState(Pstate.TERMINATED);

            gantt.append(String.format("| P%-2d ", p.getProcessId()));
            ganttTimes.append(String.format("%-6d", startTime[idx]));

            int runDuration = p.getBurstTime();
            for (PCB waiting : waitingQueue) {
                int wIdx = indexOf(waiting);
                waitedSoFar[wIdx] += runDuration;
            }

            currentTime = terminationTime[idx];
        }

        gantt.append("|");
        ganttTimes.append(currentTime);

        printGantt(gantt, ganttTimes);
        printTable(n, startTime, terminationTime, waitingTime, turnaroundTime);
        printAverages(n, waitingTime, turnaroundTime);
        printStarvationReport(starved);
    }

    private int indexOf(PCB p) {
        for (int i = 0; i < processes.size(); i++) {
            if (processes.get(i).getProcessId() == p.getProcessId()) {
                return i;
            }
        }
        return -1;
    }

    private void printGantt(StringBuilder gantt, StringBuilder ganttTimes) {
        System.out.println("Gantt Chart:");
        System.out.println(gantt);
        System.out.println(ganttTimes);
        System.out.println();
    }

    private void printTable(int n, int[] startTime, int[] terminationTime, int[] waitingTime, int[] turnaroundTime) {

        String header = String.format("%-12s %-12s %-12s %-18s %-14s %-16s",
                "Process ID", "Burst Time", "Start Time", "Termination Time", "Waiting Time", "Turnaround Time");

        String divider = "";
        for (int i = 0; i < header.length(); i++) divider += "-";

        System.out.println("Results Table:");
        System.out.println(divider);
        System.out.println(header);
        System.out.println(divider);

        for (int i = 0; i < n; i++) {
            PCB p = processes.get(i);
            System.out.printf("%-12d %-12d %-12d %-18d %-14d %-16d%n",
                    p.getProcessId(), p.getBurstTime(), startTime[i],
                    terminationTime[i], waitingTime[i], turnaroundTime[i]);
        }

        System.out.println(divider);
        System.out.println();
    }

    private void printAverages(int n, int[] waitingTime, int[] turnaroundTime) {
        double avgWaiting = 0;
        double avgTurnaround = 0;

        for (int i = 0; i < n; i++) {
            avgWaiting += waitingTime[i];
            avgTurnaround += turnaroundTime[i];
        }

        avgWaiting /= n;
        avgTurnaround /= n;

        System.out.printf("Average Waiting Time    : %.2f ms%n", avgWaiting);
        System.out.printf("Average Turnaround Time : %.2f ms%n", avgTurnaround);
        System.out.println();
    }

    private void printStarvationReport(boolean[] starved) {
        System.out.println("Starvation Report:");
        boolean anyStarved = false;
        for (int i = 0; i < processes.size(); i++) {
            if (starved[i]) {
                System.out.println("Process P" + processes.get(i).getProcessId()
                        + " suffered from starvation. Aging was applied.");
                anyStarved = true;
            }
        }
        if (!anyStarved) {
            System.out.println("No processes suffered from starvation.");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        List<PCB> readyQueue = new ArrayList<>();
        readyQueue.add(new PCB(1, 25, 4, 500));
        readyQueue.add(new PCB(2, 13, 3, 700));
        readyQueue.add(new PCB(3, 20, 3, 100));
        readyQueue.add(new PCB(4, 18, 2, 200));

        PriorityScheduler ps = new PriorityScheduler(readyQueue);
        ps.run();
    }
}