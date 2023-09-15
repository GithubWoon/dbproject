import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement; // 동적 쿼리를 하기위해 사용되는 클래스
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map; // 키와 값을 저장하는 자료구조
import java.util.Scanner;

public class Main {
    private static Map<String, Integer> products = new HashMap<>();
    private static Scanner scanner = new Scanner(System.in);

    private static final String DATABASE_URL = "jdbc:oracle:thin:@192.168.0.86:1521:xe";
    private static final String DATABASE_USERNAME = "projectss";
    private static final String DATABASE_PASSWORD = "projectss1";

    public static void main(String[] args) {
        loadProductData(); // 프로그램 시작 시 데이터를 데이터베이스에서 로드

        while (true) {
            System.out.println("로그인 시스템");
            System.out.print("아이디: ");
            String username = scanner.nextLine();
            System.out.print("비밀번호: ");
            String password = scanner.nextLine();

            if (login(username, password)) {
                if (username.equals("사용자") || username.startsWith("userid")) {
                    userMenu();
                } else if (username.equals("관리자")) {
                    adminMenu();
                }
            } else {
                System.out.println("잘못된 아이디 또는 비밀번호입니다. 다시 시도하세요.");
            }
        }
    }
    
    // 데이터베이스 연결
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
    }

    // 상품정보 데이터베이스에 등록
    private static void insertProduct(String productNumber, String productName, int quantity) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO productinfo (PRODUCTNUMBER, PRODUCTNAME, QUANTITY) VALUES (?, ?, ?)")) {
            preparedStatement.setString(1, productNumber);
            preparedStatement.setString(2, productName);
            preparedStatement.setInt(3, quantity);
            preparedStatement.executeUpdate();
            System.out.println("상품 정보가 데이터베이스에 등록되었습니다.");
        } catch (SQLException e) {
            System.out.println("데이터베이스 등록 중 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }

    // 데이터베이스에서 주문가능한 상품 불러오기
    private static void loadProductData() {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM productinfo");
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                String productName = resultSet.getString("PRODUCTName");
                int quantity = resultSet.getInt("QUANTITY");
                products.put(productName, quantity);
            }
        } catch (SQLException e) {
            System.out.println("데이터를 불러오는 중 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }

    // 상품정보 초기화
    private static void resetProductData() {
        System.out.print("상품 정보를 초기화하시겠습니까? (y/n): ");
        String response = scanner.nextLine().trim().toLowerCase();

        if (response.equals("y")) {
            products.clear();
            System.out.println("상품 정보가 초기화되었습니다.");

            // 상품정보 모두 초기화 메서드
            deleteAllProductData();
        } else {
            System.out.println("상품 정보 초기화를 취소했습니다.");
        }
    }

    // 데이터베이스에서 모든 상품 데이터 삭제
    private static void deleteAllProductData() {
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM productinfo")) {
            preparedStatement.executeUpdate();
            System.out.println("데이터베이스의 모든 상품 정보가 삭제되었습니다.");
        } catch (SQLException e) {
            System.out.println("데이터베이스에서 상품 정보를 삭제하는 중 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }

    // 상품주문
    private static void orderProduct() {
        System.out.println("\n주문 가능한 상품 목록: " + products.keySet());
        System.out.print("주문할 상품: ");
        String productName = scanner.nextLine();

        // 데이터베이스에서 해당 상품의 재고 수량 조회
        int availableQuantity = getProductQuantity(productName);

        if (availableQuantity == -1) {
            System.out.println("주문 가능한 상품이 아닙니다.");
        } else {
            System.out.println(productName + "의 현재 재고 수량: " + availableQuantity);
            System.out.print("수량: ");
            int quantity = scanner.nextInt();
            scanner.nextLine();

            if (availableQuantity >= quantity) {
                int remainingQuantity = availableQuantity - quantity;
                products.put(productName, remainingQuantity);
                System.out.println("주문이 완료되었습니다. 잔여 수량: " + remainingQuantity);
            } else {
                System.out.println("주문할 수량이 재고를 초과합니다.");
            }
        }
    }

    // 데이터베이스에서 상품의 재고 수량 조회
    private static int getProductQuantity(String productName) {
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT QUANTITY FROM productinfo WHERE PRODUCTNAME = ?")) {
            preparedStatement.setString(1, productName);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("QUANTITY");
            } else {
                return -1; // 해당 상품이 존재하지 않음
            }
        } catch (SQLException e) {
            System.out.println("상품 정보를 조회하는 중 오류가 발생했습니다.");
            e.printStackTrace();
            return -1;
        }
    }


    // 새로운 상품정보 등록
    private static void addProduct() {
        System.out.print("상품 코드: ");
        String productCode = scanner.nextLine();

        if (products.containsKey(productCode)) {
            System.out.println("이미 존재하는 상품 코드입니다. 다른 코드를 입력하세요.");
            return;
        }

        System.out.print("상품명: ");
        String productName = scanner.nextLine();
        System.out.print("재고 수량: ");
        int initialQuantity = scanner.nextInt();
        scanner.nextLine();

        insertProduct(productCode, productName, initialQuantity);
        products.put(productCode, initialQuantity);
    }

    // 사용자 메뉴
    private static void userMenu() {
        while (true) {
            System.out.println("\n사용자 메뉴");
            System.out.println("1. 주문하기");
            System.out.println("2. 종료");
            System.out.print("선택: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    orderProduct();
                    break;
                case 2:
                    saveProductData();
                    System.exit(0);
                default:
                    System.out.println("올바른 옵션을 선택하세요.");
            }
        }
    }

    // 관리자 메뉴
    private static void adminMenu() {
        while (true) {
            System.out.println("\n관리자 메뉴");
            System.out.println("1. 상품 정보 등록");
            System.out.println("2. 초기화");
            System.out.println("3. 종료");
            System.out.print("선택: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    addProduct();
                    break;
                case 2:
                    resetProductData();
                    break;
                case 3:
                    saveProductData();
                    System.exit(0);
                default:
                    System.out.println("올바른 옵션을 선택하세요.");
            }
        }
    }

    // 상품 데이터 저장
    private static void saveProductData() {
        try (Connection connection = getConnection()) {
            for (Map.Entry<String, Integer> entry : products.entrySet()) {
                String productName = entry.getKey();
                int quantity = entry.getValue();
                updateProduct(productName, quantity, connection);
            }
        } catch (SQLException e) {
            System.out.println("데이터를 저장하는 중 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }
    
    // 상품정보 업데이트 (상품정보 등록한 후 종료할때 null 값으로 등록되는거 방지)
    private static void updateProduct(String productName, int quantity, Connection connection) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE productinfo SET QUANTITY = ? WHERE PRODUCTNAME = ?")) {
            preparedStatement.setInt(1, quantity);
            preparedStatement.setString(2, productName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("데이터베이스에서 상품 정보를 업데이트하는 중 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }
    

    // 로그인 처리 (이외 사용자는 안됨)
    private static boolean login(String username, String password) {
        // 사용자 및 관리자 로그인 정보 추가
        Map<String, String> userLoginInfo = new HashMap<>();
        userLoginInfo.put("사용자", "pass1");
        userLoginInfo.put("userid001", "pass1");
        userLoginInfo.put("userid002", "pass2");
        userLoginInfo.put("userid003", "pass3");
        userLoginInfo.put("userid004", "pass4");
        userLoginInfo.put("userid005", "pass5");
        Map<String, String> adminLoginInfo = new HashMap<>();
        adminLoginInfo.put("관리자", "1");

        return (userLoginInfo.containsKey(username) && userLoginInfo.get(username).equals(password)) ||
               (adminLoginInfo.containsKey(username) && adminLoginInfo.get(username).equals(password));
    }
}