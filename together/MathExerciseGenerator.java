import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathExerciseGenerator {
    private static final String[] OPERATORS = {"+", "-", "×", "÷"};
    private static final Pattern FRACTION_PATTERN = Pattern.compile("(\\d+')?(\\d+/\\d+|\\d+)");
    // 定义目标保存路径（你的指定目录）
    private static final String TARGET_PATH = "C:\\Users\\17492\\Desktop\\math\\";

    public static void main(String[] args) {
        try {
            // 确保目标目录存在，不存在则创建
            File targetDir = new File(TARGET_PATH);
            if (!targetDir.exists()) {
                targetDir.mkdirs(); // 创建多级目录（如果需要）
            }

            if (args.length == 0) {
                printHelp();
                return;
            }

            // 处理批改模式
            if (Arrays.asList(args).contains("-e") && Arrays.asList(args).contains("-a")) {
                String exerciseFile = getArgValue(args, "-e");
                String answerFile = getArgValue(args, "-a");
                if (exerciseFile == null || answerFile == null) {
                    System.err.println("错误：缺少文件名参数");
                    printHelp();
                    return;
                }
                gradeExercises(exerciseFile, answerFile);
                System.out.println("批改完成，结果已保存到 " + TARGET_PATH + "Grade.txt");
                return;
            }

            // 处理生成题目模式
            String rStr = getArgValue(args, "-r");
            if (rStr == null) {
                System.err.println("错误：必须提供-r参数");
                printHelp();
                return;
            }

            int r;
            try {
                r = Integer.parseInt(rStr);
                if (r < 1) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                System.err.println("错误：范围必须为正整数");
                printHelp();
                return;
            }

            String nStr = getArgValue(args, "-n");
            int n = 10; // 默认生成10个题目
            if (nStr != null) {
                try {
                    n = Integer.parseInt(nStr);
                    if (n < 1 || n > 10000) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    System.err.println("错误：题目数量必须在1到10000之间");
                    printHelp();
                    return;
                }
            }

            // 生成题目
            System.out.println("正在生成" + n + "个题目，数值范围为" + r + "...");
            List<String> exercises = new ArrayList<>();
            List<String> answers = new ArrayList<>();
            Set<String> seenExpressions = new HashSet<>();

            while (exercises.size() < n) {
                ExpressionResult exprResult = generateExpression(r);
                if (exprResult == null) continue;

                String normalized = normalizeExpression(exprResult.expression);
                if (!seenExpressions.contains(normalized)) {
                    seenExpressions.add(normalized);
                    exercises.add(exprResult.expression + " =");
                    answers.add(exprResult.result);
                }
            }

            // 保存题目和答案到指定路径
            String exercisePath = TARGET_PATH + "Exercises.txt";
            String answerPath = TARGET_PATH + "Answers.txt";
            saveToFile(exercisePath, exercises);
            saveToFile(answerPath, answers);
            System.out.println("题目已保存到 " + exercisePath);
            System.out.println("答案已保存到 " + answerPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getArgValue(String[] args, String argName) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(argName) && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return null;
    }

    private static void printHelp() {
        System.out.println("用法: java MathExerciseGenerator [选项]");
        System.out.println("选项:");
        System.out.println("  -n <数量>      生成题目的数量");
        System.out.println("  -r <范围>      题目中数值的范围（必须提供）");
        System.out.println("  -e <文件>      题目文件（用于批改）");
        System.out.println("  -a <文件>      答案文件（用于批改）");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  生成10个10以内的题目（保存到指定目录）:");
        System.out.println("    java MathExerciseGenerator -n 10 -r 10");
        System.out.println("  批改指定路径的题目:");
        System.out.println("    java MathExerciseGenerator -e \"C:\\Users\\17492\\Desktop\\math\\Exercises.txt\" -a \"C:\\Users\\17492\\Desktop\\math\\Answers.txt\"");
    }

    private static ExpressionResult generateExpression(int range) {
        Random random = new Random();
        int numOps = random.nextInt(3) + 1; // 1-3个运算符

        List<Fraction> numbers = new ArrayList<>();
        List<String> ops = new ArrayList<>();

        // 生成数字和运算符
        numbers.add(generateNumber(range));
        for (int i = 0; i < numOps; i++) {
            ops.add(OPERATORS[random.nextInt(OPERATORS.length)]);
            numbers.add(generateNumber(range));
        }

        // 验证表达式合法性并计算结果
        Fraction current = numbers.get(0);
        boolean valid = true;

        for (int i = 0; i < numOps; i++) {
            String op = ops.get(i);
            Fraction next = numbers.get(i + 1);

            if (op.equals("-")) {
                if (current.compareTo(next) < 0) {
                    valid = false;
                    break;
                }
                current = current.subtract(next);
            } else if (op.equals("+")) {
                current = current.add(next);
            } else if (op.equals("×")) {
                current = current.multiply(next);
            } else if (op.equals("÷")) {
                if (next.isZero()) {
                    valid = false;
                    break;
                }
                current = current.divide(next);
                // 检查除法结果是否为合法分数
                if (current.denominator > range) {
                    valid = false;
                    break;
                }
            }

            if (current.numerator < 0) {
                valid = false;
                break;
            }
        }

        if (!valid) {
            return null;
        }

        // 构建表达式字符串
        StringBuilder expr = new StringBuilder();
        expr.append(numbers.get(0).toString());

        for (int i = 0; i < numOps; i++) {
            expr.append(" ").append(ops.get(i)).append(" ").append(numbers.get(i + 1).toString());
        }

        // 随机添加括号
        if (numOps >= 2 && random.nextBoolean()) {
            List<String> parts = new ArrayList<>();
            parts.add(numbers.get(0).toString());
            for (int i = 0; i < numOps; i++) {
                parts.add(ops.get(i));
                parts.add(numbers.get(i + 1).toString());
            }

            int start = random.nextInt(numOps);
            int end = start + 1 + random.nextInt(numOps - start);

            // 插入括号
            parts.add(start * 2, "(");
            parts.add(end * 2 + 1, ")");

            // 重建表达式
            expr = new StringBuilder();
            for (String part : parts) {
                expr.append(part).append(" ");
            }
            expr.setLength(expr.length() - 1); // 移除最后一个空格

            // 重新计算带括号的表达式结果
            try {
                current = evaluateExpression(expr.toString());
            } catch (Exception e) {
                return null;
            }
        }

        return new ExpressionResult(expr.toString(), current.toString());
    }

    private static Fraction generateNumber(int range) {
        Random random = new Random();
        boolean isFraction = random.nextBoolean();

        if (!isFraction) {
            // 生成自然数
            return new Fraction(random.nextInt(range), 1);
        } else {
            // 生成真分数
            int denominator = random.nextInt(range - 1) + 2; // 2到range-1
            int numerator = random.nextInt(denominator - 1) + 1; // 1到denominator-1

            // 可能生成带整数部分的分数
            if (random.nextBoolean() && range > 1) {
                int integerPart = random.nextInt(range - 1) + 1;
                numerator += integerPart * denominator;
            }

            return new Fraction(numerator, denominator);
        }
    }

    private static Fraction evaluateExpression(String expr) {
        // 移除所有括号
        String processed = expr.replaceAll("[()]", "");
        String[] tokens = processed.split(" ");

        // 先处理乘除
        List<Object> list = new ArrayList<>();
        for (String token : tokens) {
            if (token.matches("[×÷]")) {
                Fraction a = (Fraction) list.remove(list.size() - 1);
                Fraction b = parseFraction(tokens[list.size() / 2 + 1]);
                if (token.equals("×")) {
                    list.add(a.multiply(b));
                } else {
                    list.add(a.divide(b));
                }
            } else if (isOperator(token)) {
                list.add(token);
            } else {
                list.add(parseFraction(token));
            }
        }

        // 再处理加减
        Fraction result = (Fraction) list.get(0);
        for (int i = 1; i < list.size(); i += 2) {
            String op = (String) list.get(i);
            Fraction num = (Fraction) list.get(i + 1);

            if (op.equals("+")) {
                result = result.add(num);
            } else if (op.equals("-")) {
                result = result.subtract(num);
            }
        }

        return result;
    }

    private static boolean isOperator(String token) {
        return Arrays.asList(OPERATORS).contains(token);
    }

    private static Fraction parseFraction(String s) {
        if (s.contains("'")) {
            String[] parts = s.split("'");
            int integer = Integer.parseInt(parts[0]);
            String[] fracParts = parts[1].split("/");
            int numerator = Integer.parseInt(fracParts[0]);
            int denominator = Integer.parseInt(fracParts[1]);
            return new Fraction(numerator + integer * denominator, denominator);
        } else if (s.contains("/")) {
            String[] parts = s.split("/");
            return new Fraction(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } else {
            return new Fraction(Integer.parseInt(s), 1);
        }
    }

    private static String normalizeExpression(String expr) {
        // 移除括号
        String normalized = expr.replaceAll("[()]", "");

        // 处理加法和乘法的交换性
        for (String op : new String[]{"+", "×"}) {
            boolean changed;
            do {
                changed = false;
                Matcher matcher = Pattern.compile("(\\S+) " + op + " (\\S+)").matcher(normalized);
                if (matcher.find()) {
                    String a = matcher.group(1);
                    String b = matcher.group(2);
                    String replacement;

                    // 比较两个操作数，确保一致的顺序
                    if (compareFractions(a, b) > 0) {
                        replacement = b + " " + op + " " + a;
                    } else {
                        replacement = a + " " + op + " " + b;
                    }

                    if (!replacement.equals(matcher.group(0))) {
                        normalized = normalized.substring(0, matcher.start()) + replacement + normalized.substring(matcher.end());
                        changed = true;
                    }
                }
            } while (changed);
        }

        return normalized;
    }

    private static int compareFractions(String a, String b) {
        return parseFraction(a).compareTo(parseFraction(b));
    }

    private static void saveToFile(String filename, List<String> content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String line : content) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private static void gradeExercises(String exerciseFile, String answerFile) throws IOException {
        List<String> exercises = readFile(exerciseFile);
        List<String> userAnswers = readFile(answerFile);

        List<Integer> correct = new ArrayList<>();
        List<Integer> wrong = new ArrayList<>();

        for (int i = 0; i < exercises.size(); i++) {
            if (i >= userAnswers.size()) {
                wrong.add(i + 1);
                continue;
            }

            String exercise = exercises.get(i).replace(" =", "");
            String userAnswer = userAnswers.get(i);

            try {
                Fraction correctAnswer = evaluateExpression(exercise);
                Fraction userFraction = parseFraction(userAnswer);

                if (correctAnswer.equals(userFraction)) {
                    correct.add(i + 1);
                } else {
                    wrong.add(i + 1);
                }
            } catch (Exception e) {
                wrong.add(i + 1);
            }
        }

        // 批改结果也保存到指定目录
        String gradePath = TARGET_PATH + "Grade.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(gradePath))) {
            writer.write(formatGradeLine("Correct", correct));
            writer.newLine();
            writer.write(formatGradeLine("Wrong", wrong));
        }
    }

    private static String formatGradeLine(String type, List<Integer> indices) {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(": ").append(indices.size());

        if (!indices.isEmpty()) {
            sb.append(" (");
            for (int i = 0; i < indices.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(indices.get(i));
            }
            sb.append(")");
        }

        return sb.toString();
    }

    private static List<String> readFile(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    static class ExpressionResult {
        String expression;
        String result;

        ExpressionResult(String expression, String result) {
            this.expression = expression;
            this.result = result;
        }
    }

    static class Fraction {
        long numerator;
        long denominator;

        Fraction(long numerator, long denominator) {
            if (denominator == 0) {
                throw new ArithmeticException("分母不能为零");
            }

            // 确保分母为正数
            if (denominator < 0) {
                numerator = -numerator;
                denominator = -denominator;
            }

            // 简化分数
            long gcd = gcd(Math.abs(numerator), Math.abs(denominator));
            this.numerator = numerator / gcd;
            this.denominator = denominator / gcd;
        }

        boolean isZero() {
            return numerator == 0;
        }

        Fraction add(Fraction other) {
            long newNumerator = this.numerator * other.denominator + other.numerator * this.denominator;
            long newDenominator = this.denominator * other.denominator;
            return new Fraction(newNumerator, newDenominator);
        }

        Fraction subtract(Fraction other) {
            long newNumerator = this.numerator * other.denominator - other.numerator * this.denominator;
            long newDenominator = this.denominator * other.denominator;
            return new Fraction(newNumerator, newDenominator);
        }

        Fraction multiply(Fraction other) {
            long newNumerator = this.numerator * other.numerator;
            long newDenominator = this.denominator * other.denominator;
            return new Fraction(newNumerator, newDenominator);
        }

        Fraction divide(Fraction other) {
            long newNumerator = this.numerator * other.denominator;
            long newDenominator = this.denominator * other.numerator;
            return new Fraction(newNumerator, newDenominator);
        }

        int compareTo(Fraction other) {
            long thisVal = this.numerator * other.denominator;
            long otherVal = other.numerator * this.denominator;
            return Long.compare(thisVal, otherVal);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Fraction fraction = (Fraction) obj;
            return numerator == fraction.numerator && denominator == fraction.denominator;
        }

        @Override
        public String toString() {
            if (denominator == 1) {
                return String.valueOf(numerator);
            }

            long integerPart = numerator / denominator;
            long remainder = numerator % denominator;

            if (integerPart != 0) {
                if (remainder == 0) {
                    return String.valueOf(integerPart);
                } else {
                    return integerPart + "'" + remainder + "/" + denominator;
                }
            } else {
                return numerator + "/" + denominator;
            }
        }

        private long gcd(long a, long b) {
            return b == 0 ? a : gcd(b, a % b);
        }
    }
}