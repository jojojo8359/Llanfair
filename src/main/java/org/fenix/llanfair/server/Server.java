package org.fenix.llanfair.server;

import org.fenix.llanfair.Actions;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class Server implements Runnable {
    private Actions actions;

    private boolean doStop = false;

    private int port = 9991;

    public Server(Actions actions) {
        this.actions = actions;
    }

    public synchronized void doStop() {
        this.doStop = true;
    }

    private synchronized boolean keepRunning() {
        return !this.doStop;
    }

    @Override
    public void run() {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            Socket connectionSocket = serverSocket.accept();

            InputStream input = connectionSocket.getInputStream();

            Scanner scanner = new Scanner(input, "UTF-8");

            actions.showConnected();

            while(keepRunning() && scanner.hasNextLine()) {
                if(!keepRunning()) break;
                String line = scanner.nextLine();
//                System.out.println("Received: " + line);
                actions.processServerEvent(processMessage(line));

                if(line.toLowerCase().trim().equals("exit")) {
                    actions.showDisconnected();
                    break;
                }
            }
            if(!scanner.hasNextLine()) {
                actions.showDisconnected();
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public String getPort() {
        return Integer.toString(this.port);
    }

    /*
        Server messages must be formatted in the following format:
        "c:0000000000000"
        where c is the event code and the 13 zeroes represent the nanosecond time of the action
     */
    private ServerEvent processMessage(String message) {
        if(message.length() == 0 || message.length() == 1) {
            return new ServerEvent();
        }
        String[] parts = message.split(":");
        if(parts[0].length() > 1 && parts[0].charAt(0) == (char) 0) {
            parts[0] = String.valueOf(parts[0].charAt(1));
        }
        int code;
        long nano;
        try {
            code = Integer.parseInt(parts[0]);
            nano = Long.parseLong(parts[1]);
        } catch(NumberFormatException e) {
            e.printStackTrace();
            return new ServerEvent();
        }

        return new ServerEvent(code, nano);
    }
}
