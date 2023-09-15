import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static Map<String, Integer> products = new HashMap<>();
    private static Scanner scanner = new Scanner(System.in);

    // 데이터베이스 연결 정보
    private static final String DB_URL = "jdbc:oracle:thin:@192.168.0.86:1521:xe"; // 호스트와 포트 설정
    private static final String DB_USERNAME = "projectss";
    private static final String DB_PASSWORD = "projectss1";

    // 데이터베이스 연결 메서드
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }

    // 상품 정보를 데이터베이스에 등록하는 메서드
    private static void insertProductToDatabase(String productNumber, String productName, int quantity) {
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

    // 프로그램 시작 시 데이터를 데이터베이스에서 로드
    private static void loadProductData() {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM productinfo");
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                String productNumber = resultSet.getString("PRODUCTNUMBER");
                int quantity = resultSet.getInt("QUANTITY");
                products.put(productNumber, quantity);
            }
        } catch (SQLException e) {
            System.out.println("데이터를 불러오는 중 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }

    // 상품 정보 초기화
    private static void resetProductData() {
        System.out.print("상품 정보를 초기화하시겠습니까? (y/n): ");
        String response = scanner.nextLine().trim().toLowerCase();

        if (response.equals("y")) {
            products.clear(); // 모든 상품 정보 삭제
            System.out.println("상품 정보가 초기화되었습니다.");
        } else {
            System.out.println("상품 정보 초기화를 취소했습니다.");
        }
    }

    // 상품 주문
    private static void orderProduct() {
        System.out.println("\n주문 가능한 상품 목록: " + products.keySet());
        System.out.print("상품 코드: ");
        String productCode = scanner.nextLine();
        System.out.print("수량: ");
        int quantity = scanner.nextInt();
        scanner.nextLine(); // 개행 문자 처리

        if (products.containsKey(productCode) && products.get(productCode) >= quantity) {
            int remainingQuantity = products.get(productCode) - quantity;
            products.put(productCode, remainingQuantity);
            System.out.println("주문이 완료되었습니다. 재고 수량: " + remainingQuantity);
        } else {
            System.out.println("주문할 수 없거나 재고가 부족합니다.");
        }
    }

    // 상품 정보 등록
    private static void addProduct() {
        System.out.print("상품 코드: ");
        String productCode = scanner.nextLine();

        // 중복된 상품 코드가 이미 존재하는지 확인
        if (products.containsKey(productCode)) {
            System.out.println("이미 존재하는 상품 코드입니다. 다른 코드를 입력하세요.");
            return; // 중복된 경우 메서드를 종료
        }

        System.out.print("상품명: ");
        String productName = scanner.nextLine();
        System.out.print("재고 수량: ");
        int initialQuantity = scanner.nextInt();
        scanner.nextLine(); // 개행 문자 처리

        // 데이터베이스에 상품 정보 등록
        insertProductToDatabase(productCode, productName, initialQuantity);

        // products 맵에도 상품 정보 추가
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
            scanner.nextLine(); // 개행 문자 처리

            switch (choice) {
                case 1:
                    orderProduct();
                    break;
                case 2:
                    saveProductData(); // 프로그램 종료 시 데이터를 파일에 저장
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
            scanner.nextLine(); // 개행 문자 처리

            switch (choice) {
                case 1:
                    addProduct();
                    break;
                case 2:
                    resetProductData(); // 상품 정보 초기화
                    break;
                case 3:
                    saveProductData(); // 프로그램 종료 시 데이터를 파일에 저장
                    System.exit(0);
                default:
                    System.out.println("올바른 옵션을 선택하세요.");
            }
        }
    }

    // 데이터 저장
private static void saveProductData() {
    try (Connection connection = getConnection()) {
        for (Map.Entry<String, Integer> entry : products.entrySet()) {
            String productNumber = entry.getKey();
            int quantity = entry.getValue();
            String productName = getProductNameFromDatabase(productNumber); // 상품명 가져오기
            updateProductInDatabase(productNumber, productName, quantity, connection);
        }
    } catch (SQLException e) {
        System.out.println("데이터를 저장하는 중 오류가 발생했습니다.");
        e.printStackTrace();
    }
}

// 데이터베이스에서 상품명을 가져오는 메서드
private static String getProductNameFromDatabase(String productNumber) {
    String productName = null;
    try (Connection connection = getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement("SELECT PRODUCTNAME FROM productinfo WHERE PRODUCTNUMBER = ?");
    ) {
        preparedStatement.setString(1, productNumber);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            productName = resultSet.getString("PRODUCTNAME");
        }
    } catch (SQLException e) {
        System.out.println("데이터베이스에서 상품명을 가져오는 중 오류가 발생했습니다.");
        e.printStackTrace();
    }
    return productName;
}

// 데이터베이스에서 기존 상품 정보를 업데이트하는 메서드
private static void updateProductInDatabase(String productNumber, String productName, int quantity, Connection connection) {
    try (PreparedStatement preparedStatement = connection.prepareStatement(
            "UPDATE productinfo SET PRODUCTNAME = ?, QUANTITY = ? WHERE PRODUCTNUMBER = ?")) {
        preparedStatement.setString(1, productName);
        preparedStatement.setInt(2, quantity);
        preparedStatement.setString(3, productNumber);
        preparedStatement.executeUpdate();
    } catch (SQLException e) {
        System.out.println("데이터베이스에서 상품 정보를 업데이트하는 중 오류가 발생했습니다.");
        e.printStackTrace();
    }
}

    public static void main(String[] args) {
        loadProductData(); // 프로그램 시작 시 데이터를 데이터베이스에서 로드

        while (true) {
            System.out.println("로그인 시스템");
            System.out.print("아이디: ");
            String username = scanner.nextLine();
            System.out.print("비밀번호: ");
            String password = scanner.nextLine();

            if (login(username, password)) {
                if (username.equals("사용자")) {
                    userMenu();
                } else if (username.equals("관리자")) {
                    adminMenu();
                }
            } else {
                System.out.println("잘못된 아이디 또는 비밀번호입니다. 다시 시도하세요.");
            }
        }
    }

    private static boolean login(String username, String password) {
        return (username.equals("사용자") || username.equals("관리자")) && password.equals("1");
    }
}