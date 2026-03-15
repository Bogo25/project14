//Author: Bogomil M. Iliev

import java.util.Scanner;

public class Application {
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.print("NFA> ");
        String[] input = sc.nextLine().split(" ");
        while (!input[0].equalsIgnoreCase("exit")) {
            switch (input[0].toLowerCase()) {
                case "help":
//                    File help = new File("help");
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
            }




            System.out.print("NFA> ");
            input = sc.nextLine().split(" ");
        }
    }
}