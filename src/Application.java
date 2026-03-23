//Author: Bogomil M. Iliev

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Application {
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.print("NFA> ");
        String[] input = sc.nextLine().split(" ");
        while (!input[0].equalsIgnoreCase("exit")) {
            switch (input[0].toLowerCase()) {
                case "help":
                    System.out.println(getHelp());
                    break;
                case "save":
                    ;
                case "save as":
                    ;
                case "open":
                    ;
//                case "close":
//                    ;
                case "list":
                    ;
                case "print":
                    ;
                case "empty":
                    ;
                case "deterministic":
                    ;
                case "recognize":
                    ;
                case "union":
                    ;
                case "concat":
                    ;
                case "un":
                    ;
                case "reg":
                    ;
                default:
                    System.out.println("Invalid command");
            }




            System.out.print("NFA> ");
            input = sc.nextLine().split(" ");
        }
    }

    public static String getHelp() {
        try {
            return Files.readString(Paths.get(System.getProperty("user.dir"),"src","help"));
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}