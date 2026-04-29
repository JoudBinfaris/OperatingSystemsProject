package proj;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Queue;

public class FileReaderThread implements Runnable {
	private String fileName;
	private Queue<PCB> jobQueue;

	public FileReaderThread(String fileName, Queue<PCB> jobQueue) {
		this.fileName = fileName;
		this.jobQueue = jobQueue;
	}

	@Override
	public void run() {
		System.out.println("Thread 1: reading file " + fileName);

		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line;

			while ((line = br.readLine()) != null) {
				line = line.trim();

				//skip empty lines and start and end
				if (line.isEmpty() || line.startsWith("[")) {
					continue;
				}

				//process ID: burst time in ms: priority; Memory required in MB
				//1:25:4;500

				// split by ; to separate the memory required
				String[] parts = line.split(";");
				if (parts.length != 2)
					continue; // skip lines with mistakes

				int memoryRequired = Integer.parseInt(parts[1].trim());

				// split the first half by : to get ID burst and priority
				String[] processInfo = parts[0].split(":");
				if (processInfo.length != 3)
					continue;

				int processId = Integer.parseInt(processInfo[0].trim());
				int burstTime = Integer.parseInt(processInfo[1].trim());
				int priority = Integer.parseInt(processInfo[2].trim());

				//create PCB object
				PCB newProcess = new PCB(processId, burstTime, priority, memoryRequired);

				//add to jobQueue
				synchronized (jobQueue) {
				    jobQueue.add(newProcess);
				    
				//wake up Thread 2 if it was sleeping 
				    jobQueue.notifyAll(); 
				}

				System.out.println("Thread 1: Loaded " + newProcess.toString());
			}
		} catch (IOException e) {
			System.err.println("Thread 1 Error: Could not read file -> " + e.getMessage());
		} catch (NumberFormatException e) {
			System.err.println("Thread 1 Error: Malformed number in file -> " + e.getMessage());
		}

		System.out.println("Thread 1: finished loading processes into Job Queue.");
	}
}
