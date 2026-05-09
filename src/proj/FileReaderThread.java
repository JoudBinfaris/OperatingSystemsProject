package proj;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Queue;

public class FileReaderThread implements Runnable {
    private String fileName;
    private Queue<PCB> jobQueue;
    private MemoryManager memoryManager;

    public FileReaderThread(String fileName, Queue<PCB> jobQueue, MemoryManager memoryManager) {
        this.fileName = fileName;
        this.jobQueue = jobQueue;
        this.memoryManager = memoryManager;
    }

    @Override
    public void run() {
        System.out.println("Thread 1: reading file " + fileName);

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("[")) {
                    continue;
                }

                String[] parts = line.split(";");
                if (parts.length != 2) continue;

                int memoryRequired = Integer.parseInt(parts[1].trim());

                String[] processInfo = parts[0].split(":");
                if (processInfo.length != 3) continue;

                int processId = Integer.parseInt(processInfo[0].trim());
                int burstTime = Integer.parseInt(processInfo[1].trim());
                int priority = Integer.parseInt(processInfo[2].trim());

                PCB newProcess = new PCB(processId, burstTime, priority, memoryRequired);

                synchronized (jobQueue) {
                    jobQueue.add(newProcess);
                    jobQueue.notifyAll();
                }

                System.out.println("Thread 1: Loaded " + newProcess.toString());
            }
        } catch (IOException e) {
            System.err.println("Thread 1 Error: Could not read file -> " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Thread 1 Error: Malformed number in file -> " + e.getMessage());
        }

        memoryManager.setFileReaderDone();
        System.out.println("Thread 1: finished loading processes into Job Queue.");
    }
}