package com.Pauliaus;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException, ParseException {
        /*
        A program to check the remaining quantity and expiration dates of the products
        Currently, the program:
        Assumes that the column order does not change, ignores the header line
        Assumes that all of the dates entered will be in the same format
        Assumes that all product quantities are integers (not doubles)
         */
        System.out.println("+--------------------------------------+");
        System.out.println("|      Inventory Checking System       |");
        System.out.println("+--------------------------------------+");
        System.out.println("NOTE: the data file must be in the same location as your program!");
        System.out.println("Enter \"0\" to cancel/unload the data file");
        System.out.println("Please put in the full name of your data file, including the extension:");

        String dataFile = getFileName();                    //Gets a string of the files' name. File must exist in the same location as the program
        Scanner scanner = new Scanner(System.in);

        boolean running = true;                             //Makes the program loop until this is switched to false
        int selection;                                      //Stores the users choice of what program should do

        while (running) {
            System.out.println("\nHow would you like to proceed with the program?");
            System.out.println("Please enter a valid selection number - 1, 2, 3 or 0");
            System.out.println("1 - check for items that are low on quantity");
            System.out.println("2 - check for items that are nearing an expiration date");
            System.out.println("3 - re-load or load another data file");
            System.out.println("0 - exit the program");
            //Validates the user input to be an integer
            while (!scanner.hasNextInt()) {
                String string = scanner.next();
                System.out.printf("\"%s\" is not a valid selection.\n", string);
            }
            selection = scanner.nextInt();
            //Switch statement for acting upon user input
            switch (selection) {
                case 1:
                    if (dataFile == null) {                 //Checks to avoid exceptions in case the user decided not to load in a file
                        System.out.println("No data file loaded, please load before checking for minimum quantity");
                        break;
                    }
                    System.out.println("Please enter the minimum wanted amount of an item:");
                    int amount;
                    while (!scanner.hasNextInt()) {          //Validates the user input to be an integer
                        String string = scanner.next();
                        System.out.printf("\"%s\" is not a valid amount.\n", string);
                    }
                    amount = scanner.nextInt();
                    LinkedHashMap<String, Integer> lowQuantityProducts = lowQuantity(loadFile(dataFile), amount);
                    //There might not be any items below the checked quantity - no need to print a table then
                    if (lowQuantityProducts.isEmpty()) {
                        System.out.println("No items are below the specified amount");
                        break;
                    }
                    //Prints out the items stored in a hash map
                    System.out.printf("%-56s|%s\n", "Product:", "Items left:");
                    for (Map.Entry<String, Integer> product : lowQuantityProducts.entrySet()) {
                        System.out.printf("%-56s|%d\n", product.getKey(), product.getValue());
                    }
                    break;
                case 2:
                    if (dataFile == null) {         //Checks if the file has been loaded
                        System.out.println("No data file loaded, please load before checking the expiration dates");
                        break;
                    }
                    System.out.println("Please enter the date up until which to check the products:");
                    boolean checkingDate = true;
                    Date upToDate = new Date();
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    dateFormat.setLenient(false);               //Forces the date entered to strictly conform to the date format
                    while (checkingDate) {                      //Loop to validate the users' date input
                        System.out.println("Date must be in YYYY-MM-DD format, including the hyphens, 2020-01-01 etc.");
                        String date = scanner.next();
                        try {
                            upToDate = dateFormat.parse(date);  //Tries to parse the input through the date format. If it fails, throws an exception
                            checkingDate = false;
                        } catch (ParseException e) {
                            System.out.println("Invalid date entry");
                        }
                    }
                    LinkedHashMap<String, Date> expiringProducts = nearExpDate(loadFile(dataFile), upToDate);
                    //In case none of the products expire before the specified date
                    if (expiringProducts.isEmpty()) {
                        System.out.println("No items are expiring before the date specified");
                        break;
                    }
                    //Prints out the products collected in the hash map
                    System.out.printf("%-50s|%s\n", "Product:", "Expiration date:");
                    for (Map.Entry<String, Date> product : expiringProducts.entrySet()) {
                        System.out.printf("%-50s|%s\n", product.getKey(), dateFormat.format(product.getValue()));
                    }
                    break;
                case 3:
                    //Used in case user did not load the file initially, or the file was not in the programs' directory
                    System.out.println("NOTE: the data file must be in the same location as your program!");
                    System.out.println("Enter \"0\" to cancel/unload the data file");
                    System.out.println("Please put in the full name of your data file, including the extension:");
                    dataFile = getFileName();
                    break;
                case 0:
                    //Stops and closes the program
                    running = false;
                    break;
                default:
                    //If the input was an integer, but one a program could recognise
                    System.out.printf("\"%s\" is not a valid selection\n", selection);
                    break;
            }
        }
    }

    private static String getFileName() {
        Scanner scanner = new Scanner(System.in);
        String fileName = scanner.next();
        if (fileName.equals("0"))
            return null;                       //Allows the user to escape the endless loop in case no file is in the programs' directory
        File dataFile = new File(fileName);                          //Creates a File object for checking if the file the user specified exists

        if (dataFile.exists()) {
            System.out.printf("\"%s\" loaded\n", fileName);
            return fileName;                                         //Checks if the file exists in a programs' location
        } else {
            System.out.printf("Could not find \"%s\", please try again:\n", fileName);
            return getFileName();                                    //Returns itself until a valid file name is entered (or 0)
        }
    }

    private static List<String[]> loadFile(String dataFile) throws IOException {

        Stream<String> lineStream = Files.lines(Paths.get(dataFile));           //Gets a stream of all the lines in a file
        List<String[]> lineList = lineStream.skip(1)                            //Skips the header of the file - program is not checking if the order of the columns changes between files
                .sorted()                                                       //Sorts the products so that the duplicates are one after another
                .map(line -> line.split(","))                            //Splits the line into different columns
                .collect(Collectors.toList());                                  //Puts all resulting lines into a List
        //Merges the detected duplicates into a single line with updated quantity
        for (int n = 0; n < lineList.size() - 1; n++) {
            if (lineList.get(n)[0].equals(lineList.get(n + 1)[0]) &&
                    lineList.get(n)[1].equals(lineList.get(n + 1)[1]) &&
                    lineList.get(n)[3].equals(lineList.get(n + 1)[3])) {        //Checks if all columns except quantity match
                lineList.set(n, new String[]{lineList.get(n)[0], lineList.get(n)[1],
                        String.valueOf(Integer.parseInt(lineList.get(n)[2]) + Integer.parseInt(lineList.get(n + 1)[2])),
                        lineList.get(n)[3]});                                   //Creates a new string, absorbing the next strings quantity value
                lineList.remove(n + 1);                                        //Deletes the string whose quantity was absorbed
            }
        }
        return lineList;
    }

    private static LinkedHashMap<String, Integer> lowQuantity(List<String[]> productList, Integer value) {
        LinkedHashMap<String, Integer> lowQuantityProducts = new LinkedHashMap<>(); //A way to store key and value pairs without losing the order

        for (String[] product : productList) {                                      //Iterating through the product list
            if (Integer.parseInt(product[2]) < value) {                             //Checks if quantity value is lower than the user specified
                String productName = product[0] + ", code " + product[1]            //Creates a string containing product name and product code for easier differentiation
                        + ", expire on " + product[3];                             //Adds the expiration date value so that each product string is unique - maps do not support duplicate keys
                Integer remains = Integer.parseInt(product[2]);                     //Stores the remaining quantity value
                lowQuantityProducts.put(productName, remains);                      //Stores the newly created string and integer in an output map
            }
        }
        return lowQuantityProducts;
    }

    private static LinkedHashMap<String, Date> nearExpDate(List<String[]> productList, Date date) throws ParseException {
        LinkedHashMap<String, Date> nearExpDateProducts = new LinkedHashMap<>();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");          //The date format the program will be expecting to find in the data file

        for (String[] product : productList) {                                      //Iterates through all of the products
            if (dateFormat.parse(product[3]).before(date)) {                        //Checks if the products date comes before the one the user specified
                String productName = product[0] + ", code " + product[1]            //Gets the products name and code, to differentiate more easily between products
                        +", " + product[2] + " items left";                         //Adds in the quantity left to make each string created more unique
                Date expDate = dateFormat.parse(product[3]);                        //Gets the expiration date of the product
                //In case products name, code and quantity match, adds a space to the end of the string to make it unique
                if (nearExpDateProducts.containsKey(productName)) productName = productName + " ";
                nearExpDateProducts.put(productName, expDate);                      //Put the newly generated string and date as a key - value pair in map
            }
        }
        return nearExpDateProducts;
    }
}

