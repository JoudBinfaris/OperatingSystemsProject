package proj;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class RoundRobinScheduler {

    private static final int TIME_QUANTUM = 5;

    private List<PCB> processes;
    private Queue<PCB> readyQueue;

    public RoundRobinScheduler(List<PCB> readyQueue) {
        this.processes = new ArrayList<>(readyQueue);
        this.readyQueue = new LinkedList<>(readyQueue);
    }

    public void run() {

        System.out.println("\n==========================================");
        System.out.println("   Round Robin Scheduler  (q = 5 ms)     ");
        System.out.println("==========================================\n");

        int n = processes.size();

        int[] remainingBurst = new int[n];
        int[] startTime = new int[n];
        int[] terminationTime = new int[n];
        int[] waitingTime = new int[n];
        int[] turnaroundTime = new int[n];
        boolean[] hasStarted = new boolean[n];

        for (int i = 0; i < n; i++) {
            remainingBurst[i] = processes.get(i).getBurstTime();
        }

        StringBuilder gantt = new StringBuilder();
        StringBuilder ganttTimes = new StringBuilder();

        int currentTime = 0;

        while (!readyQueue.isEmpty()) {
            PCB p = readyQueue.poll();
            int idx = indexOf(p);

            p.setState(Pstate.RUNNING);

            if (!hasStarted[idx]) {
                startTime[idx] = currentTime;
                hasStarted[idx] = true;
            }

            int burstBefore = remainingBurst[idx];
            int runTime = Math.min(TIME_QUANTUM, remainingBurst[idx]);

            currentTime += runTime;
            remainingBurst[idx] -= runTime;

            int burstAfter = remainingBurst[idx];

            gantt.append(String.format("| P%d(%d->%d) ", p.getProcessId(), burstBefore, burstAfter));
            ganttTimes.append(String.format("%-12d", currentTime - runTime));

            if (remainingBurst[idx] == 0) {
                terminationTime[idx] = currentTime;
                turnaroundTime[idx] = terminationTime[idx] - p.getArrivalTime();
                waitingTime[idx] = turnaroundTime[idx] - p.getBurstTime();
                p.setWaitingTime(waitingTime[idx]);
                p.setTurnaroundTime(turnaroundTime[idx]);
                p.setState(Pstate.TERMINATED);
            } else {
                p.setState(Pstate.READY);
                readyQueue.offer(p);
            }
        }

        gantt.append("|");
        ganttTimes.append(currentTime);

        printGantt(gantt, ganttTimes);
        printTable(n, startTime, terminationTime, waitingTime, turnaroundTime);
        printAverages(n, waitingTime, turnaroundTime);
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
                "Process ID", "Burst Time", "Start Time",
                "Termination Time", "Waiting Time", "Turnaround Time");

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

    public static void main(String[] args) {
        List<PCB> readyQueue = new ArrayList<>();
        readyQueue.add(new PCB(1, 25, 4, 500));
        readyQueue.add(new PCB(2, 13, 3, 700));
        readyQueue.add(new PCB(3, 20, 3, 100));
        readyQueue.add(new PCB(4, 18, 2, 200));

        RoundRobinScheduler rr = new RoundRobinScheduler(readyQueue);
        rr.run();
    }
}