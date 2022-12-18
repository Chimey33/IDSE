import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class IDSE {

    public static void main(String[] args) {
        System.out.println();
        System.out.println();
        ArrayList<Event> EventList = new ArrayList<>();
        if (args.length < 3) {
            System.err.println("!!!!!!   WARNING    !!!!!!");
            System.err.println("Incorrect input! Command Line should take the form:");
            System.err.println("java IDSE Events.txt Base-Dats.txt Test-Events.txt ");
            System.exit(0);
        }

        run("readEvent", args[0], EventList, 0);
        run("readBase", args[1], EventList, 0);

        double threshold = EventList.stream().reduce(0.00, (count , event) -> count + event.getWeight(), Double::sum) * 2;
        EventList.forEach(ev -> {
            ev.calculateAverage();
            ev.calculateStdDev();
        });

        System.out.printf("%-30s %14s %14s %14s", "Event", "Average", "Stdev", "Weight");
        System.out.println();

        EventList.forEach(Event::print);

        System.out.println();
        System.out.println("Threshold " + threshold);
        System.out.println();

        run("runTest", args[2], EventList, threshold);

        System.out.println();
        System.out.println();
    }

    static int numEvents = 0;

    static void runFunction (String run, String filename, ArrayList<Event> eventList, double threshold) throws IOException {
        File file = new File(filename);
        switch (run) {
            case "readEvent" -> readEvents(file, eventList);
            case "readBase" -> readBase(file, eventList);
            case "runTest" -> runTest(file, eventList, threshold);
        }
    }

    static void run (String run, String filename, ArrayList<Event> eventList, double threshold) {
        try {
            runFunction(run, filename, eventList, threshold);
        } catch (FileNotFoundException e) {
            System.out.println("Could not find " + filename);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readEvents(File file, ArrayList<Event> startingList) throws IOException {
        int lineCount = 1;
        Scanner scanner = new Scanner(file);
        boolean firstLine = true;
        while (scanner.hasNext()) {
            if (firstLine) {
                String s = scanner.nextLine();

                try {
                    numEvents = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    System.out.println("Not a number");
                }

                firstLine = false;
            } else {
                lineCount++;
                String line = scanner.nextLine();

                if (countOccurrences(line) > 8) {
                    printInputWarning(line, lineCount, file, "More than 4 events detected on one line!");
                }

                String[] splitValues = line.split(":", -1);
                if (splitValues.length > 1) {
                    for (int i = 0; i < splitValues.length - 1; i = i + 2) {
                        try {
                            Double.parseDouble(splitValues[i + 1]);
                        } catch (NumberFormatException n) {
                            printInputWarning(line, lineCount, file, "Event weight must be a double");
                        }
                        Event ev = new Event(splitValues[i], Double.parseDouble(splitValues[i + 1]));
                        startingList.add(ev);
                    }
                }
            }
        }
        scanner.close();
    }

    public static void readBase(File file, ArrayList<Event> events) throws IOException {
        int lineCount = 0;
        Scanner scanner = new Scanner(file);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            lineCount++;
            validateEventOccurrence(line, lineCount, file);
            String[] splitValues;
            splitValues = line.split(":", -1);
            for (int i = 0; i < splitValues.length - 1; i++) {
                try {
                    Integer.parseInt(splitValues[i]);
                } catch (NumberFormatException n) {
                    printInputWarning(line, lineCount, file, "Input must only contain digits");
                }

                events.get(i).addToEventLog(splitValues[i]);
            }
        }
        scanner.close();
    }

    public static void runTest(File file, ArrayList<Event> events, double threshold) throws IOException {
        int lineCount = 0;
        Scanner scanner = new Scanner(file);
        int count = 1;
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            lineCount++;
            validateEventOccurrence(line, lineCount, file);
            String first = "Line " + count;
            String second = "--";
            String[] splitValues;
            double combinedDistance = 0;
            splitValues = line.split(":", -1);
            for (int i = 0; i < splitValues.length - 1; i++) {
                try {
                    events.get(i).calculateDistanceFromThreshold(Double.parseDouble(splitValues[i]));
                } catch (NumberFormatException n) {
                    printInputWarning(line, lineCount, file, "Input must only contain digits");
                }
                combinedDistance += events.get(i).calculateDistanceFromThreshold(Double.parseDouble(splitValues[i]));
            }
            int formatSize = (numEvents * 4) + 1;
            String format = "%-9s%-3s%-" + formatSize + "s %10s %-10.2f %-14s";
            System.out.printf(format, first, second, line, "Distance: ", combinedDistance, combinedDistance > threshold ? "Alarm: Yes": "Alarm: No");
            System.out.println();

            count++;
        }
        scanner.close();
    }

    static void validateEventOccurrence (String line, int lineCount, File file) {
        if (countOccurrences(line) > numEvents ) {
            printEventErrorMessage(line, lineCount, file, "More than");
        }
        if (countOccurrences(line) < numEvents) {
            printEventErrorMessage(line, lineCount, file, "Less than");
        }
    }

    static void printEventErrorMessage (String line, int lineCount, File file, String description){
        System.err.println("!!!!!!   WARNING    !!!!!!");
        System.err.println(description + numEvents + " events detected on one line in your file: " + file.getName() + "!!!");
        System.err.println("Line " + lineCount + ": " + line);
        System.exit(0);
    }

    static void printInputWarning (String line, int lineCount, File file, String errorMessage) {
        System.err.println("!!!!!!   WARNING    !!!!!!");
        System.err.println("Incorrect input detected on one line in your file: " + file.getName() + "!!!");
        System.err.println(errorMessage);
        System.err.println("Line " + lineCount + ": " + line);
        System.exit(-1);
    }

    static int countOccurrences(String s) {
        return (int) s.chars().filter(c -> c == ':').count();
    }

    static class Event {
        private final String name;
        private final double weight;
        private double stdDev;
        private double average;
        ArrayList<Integer> EventLog;

        public Event(String name, double weight) {
            this.name = name;
            this.weight = weight;
            this.EventLog = new ArrayList<>();
        }

        public void calculateAverage() {
            double total = this.EventLog.stream().reduce(0, Integer::sum);
            this.average = total / this.EventLog.size();
        }

        public void calculateStdDev() {
            List<Double> deviation = this.EventLog.stream().map(entry -> Math.pow((entry - this.average), 2)).toList();

            double total = deviation.stream().reduce(0.00, Double::sum);
            double variance = total / deviation.size();
            this.stdDev = Math.sqrt(variance);
        }

        void addToEventLog(String event) {
            this.EventLog.add(Integer.valueOf(event));
        }

        public void print() {
            System.out.printf("%-30s %14.2f %14.2f %14.2f", this.name, this.average, this.stdDev, this.weight);
            System.out.println();
        }

        public double getWeight() {
            return this.weight;
        }

        public double calculateDistanceFromThreshold(double event) {
            double distance = Math.abs(this.average - event);
            return (distance / this.stdDev) * this.weight;
        }
    }
}


