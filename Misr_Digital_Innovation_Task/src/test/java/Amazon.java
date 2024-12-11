import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class Amazon {
    public static void main(String[] args) {
        // Setup WebDriver (Firefox driver)
        WebDriverManager.firefoxdriver().setup();
        WebDriver driver = new FirefoxDriver();

        try {
            // Start of execution
            System.out.println("Starting the Amazon script...");

            // Open Amazon Egypt website
            driver.get("https://www.amazon.eg/");
            driver.manage().window().maximize();
            System.out.println("Opened Amazon Egypt website.");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(130));

            // Login
            login(driver, wait);

            // 2. Open "All" menu from the left side
            WebElement allMenu = wait.until(ExpectedConditions.elementToBeClickable(By.id("nav-hamburger-menu")));
            allMenu.click();
            System.out.println("Clicked 'All' menu button.");

            // 3. Click on "See All"
            WebElement seeAll = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//i[@class='nav-sprite hmenu-arrow-more']")));
            seeAll.click();
            System.out.println("Clicked 'See All'.");

            // 4. Click on "Video Games"
            WebElement videoGames = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(text(),'Video Games')]")));
            videoGames.click();
            System.out.println("Clicked 'Video Games'.");

            // 5. Scroll to "All Video Games" and click
            WebElement allVideoGames = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(text(),'All Video Games')]")));
            try {
                allVideoGames.click();
                System.out.println("Clicked 'All Video Games'");
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", allVideoGames);
                System.out.println("Clicked 'All Video Games' ");
            }
            // Wait for URL to change to confirm navigation
            wait.until(ExpectedConditions.urlContains("nav_em_vg_all"));
            System.out.println("URL changed, page has loaded successfully.");

            // Check if 'Video Games' section is visible
            WebElement videoGamesTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(text(),'Video Games')]")));
            System.out.println("Successfully navigated to 'Video Games' section.");

            // 6. Click on "Free Shipping" filter
            applyFilter(wait, driver, "Free Shipping");

            // 7. Click on "New" filter
            applyFilter(wait, driver, "New");

            // 8. Sort by "Price: High to Low"
            WebElement sortMenu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(text(), 'Sort by')]")));
            sortMenu.click();
            WebElement highToLow = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(), 'Price: High to Low')]")));
            highToLow.click();
            System.out.println("Clicked 'Price: High to Low' to sort.");

            // Loop through all pages, adding products to the cart if they are below 15k EGP
            boolean hasNextPage = true;
            while (hasNextPage) {
                System.out.println("Processing current page...");

                List<WebElement> products = driver.findElements(By.xpath("//div[contains(@class, 's-main-slot')]/div[@data-asin]"));
                boolean addedToCart = false;

                for (WebElement product : products) {
                    try {
                        String priceText = getProductPrice(product);
                        if (priceText != null) {
                            double price = parsePrice(priceText);
                            if (price < 15000) {
                                System.out.println("Adding product with price " + price + " EGP to cart...");
                                addItemToCart(driver, product);
                                addedToCart = true;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error retrieving product details or price.");
                    }
                }

                if (addedToCart) {
                    System.out.println("Products have been added to cart.");
                } else {
                    System.out.println("No products below 15k EGP found on this page.");
                }

                // Handle pagination to go to the next page
                try {
                    WebElement nextPageButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@class, 's-pagination-next') and contains(text(), 'Next')]")));
                    if (nextPageButton.isEnabled()) {
                        nextPageButton.click();
                        System.out.println("Clicked next page button.");
                        Thread.sleep(3000); // Wait for the page to load
                    } else {
                        System.out.println("Next page button is disabled. Exiting.");
                        hasNextPage = false; // Exit if the button is disabled
                    }
                } catch (Exception e) {
                    System.out.println("Next page button not found or interaction failed. Retrying...");
                    try {
                        // Adding a retry mechanism to deal with potential timing issues
                        WebElement retryNextPageButton = driver.findElement(By.xpath("//a[contains(@class, 's-pagination-next') and contains(text(), 'Next')]"));
                        if (retryNextPageButton.isEnabled()) {
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", retryNextPageButton);
                            System.out.println("Clicked next page.");
                            Thread.sleep(3000); // Wait for the page to load
                        } else {
                            System.out.println("Next page button is disabled after retry. Exiting.");
                            hasNextPage = false;
                        }
                    } catch (Exception retryException) {
                        System.out.println("Failed to find or click next page button even after retry. Exiting.");
                        hasNextPage = false;
                    }
                }
            }

            // === Added Steps for Checkout Process ===
            proceedToCheckout(driver, wait);

        } catch (Exception e) {
            e.printStackTrace(); // Print error for debugging
            System.out.println("An error occurred, capturing a screenshot...");

            // Capture screenshot on failure
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            try {
                ImageIO.write(ImageIO.read(screenshot), "png", new File("screenshot.png"));
                System.out.println("Screenshot captured on failure: screenshot.png");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            // Close the browser
            //driver.quit();
            System.out.println("Browser closed.");
        }
    }


    private static void proceedToCheckout(WebDriver driver, WebDriverWait wait) throws InterruptedException {

        // Step 1: Click "Go to basket"
        WebElement goToBasketButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("nav-cart")));
        goToBasketButton.click();
        System.out.println("Clicked 'Go to basket'.");
        //step 2 get priceElement
        //check price for all products
        WebElement priceElement =wait.until(ExpectedConditions.elementToBeClickable(By.xpath("(//span[@class='a-size-medium a-color-base sc-price sc-white-space-nowrap' and starts-with(text(), 'EGP')])[1]")));
        String priceText = priceElement.getText();
        System.out.println("Extracted Price: " + priceText);

        // Step 3: Click "Proceed to Buy"
        WebElement proceedToBuyButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@name='proceedToRetailCheckout']")));
        proceedToBuyButton.click();
        System.out.println("Clicked 'Proceed to Buy'.");
        // Step 4: Click "Change" for shipping address
        WebElement shippingAddressButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@id='addressChangeLinkId' and contains(text(), 'Change')]")));
        Thread.sleep(4000);  // Ensure page has loaded
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", shippingAddressButton);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", shippingAddressButton);
        System.out.println("Clicked 'Change' for shipping address.");

        // Step 5: Click "Use this address"
        WebElement useThisAddressButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@aria-labelledby='orderSummaryPrimaryActionBtn-announce']")));
        Thread.sleep(4000);  // Ensure page has loaded
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", useThisAddressButton);
        System.out.println("Clicked 'Use this address'.");
        // Step 6: Select "Cash on Delivery" payment method
        try {
            WebElement cashPaymentOption = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='cash-on-delivery']")));
            cashPaymentOption.click();
            System.out.println("Selected 'Cash on Delivery' payment method.");
        } catch (Exception e) {
            System.out.println("Error selecting payment method: " + e.getMessage());
        }
    }


    private static void login(WebDriver driver, WebDriverWait wait) {
        System.out.println("Waiting for 'Sign In' button...");
        WebElement signIn = wait.until(ExpectedConditions.elementToBeClickable(By.id("nav-link-accountList-nav-line-1")));
        signIn.click();
        System.out.println("Clicked 'Sign In' button.");
        // 1. Login
        System.out.println("Waiting for email input...");
        WebElement email = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ap_email")));
        email.sendKeys("01277989285");
        driver.findElement(By.id("continue")).click();
        System.out.println("Entered email and clicked 'Continue'.");
        System.out.println("Waiting for password input...");
        WebElement password = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ap_password")));
        password.sendKeys("Mohamed123@@@");
        driver.findElement(By.id("signInSubmit")).click();
        System.out.println("Entered password and clicked 'Sign In'.");
    }

    private static void applyFilter(WebDriverWait wait, WebDriver driver, String filterName) {
        try {
            WebElement filter = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[contains(text(), '" + filterName + "') and @class='a-size-small a-color-base a-spacing-none']")));
            filter.click();
            System.out.println("Clicked '" + filterName + "' filter.");
        } catch (Exception e) {
            System.out.println("Failed to find '" + filterName + "' filter. Attempting to click using JavaScript...");
            WebElement filter = driver.findElement(By.xpath("//span[contains(text(), '" + filterName + "')]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", filter);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", filter);
            System.out.println("Clicked '" + filterName + "' filter using JavaScript.");
        }
    }

    private static String getProductPrice(WebElement productElement) {
        try {
            WebElement priceElement = productElement.findElement(By.xpath(".//span[@class='a-price-whole']"));
            if (priceElement != null) {
                return priceElement.getText().replace(",", "").trim();
            }
        } catch (Exception e) {
            // No price found using this XPath
        }

        try {
            WebElement offscreenPrice = productElement.findElement(By.xpath(".//span[@class='a-offscreen']"));
            if (offscreenPrice != null) {
                return offscreenPrice.getText().replace("EGP", "").replace(",", "").trim();
            }
        } catch (Exception e) {
            // No price found using this XPath
        }

        return null; // Return null if no price is found
    }

    private static double parsePrice(String priceText) {
        try {
            return Double.parseDouble(priceText.trim());
        } catch (NumberFormatException e) {
            System.out.println("Error parsing price.");
            return -1;
        }
    }

    private static void addItemToCart(WebDriver driver, WebElement productElement) {
        try {
            WebElement addToCartButton = productElement.findElement(By.xpath(".//button[text()='Add to cart']"));
            addToCartButton.click();
            System.out.println("Added product to cart.");
        } catch (Exception e) {
            System.out.println("Failed to add product to cart.");
        }
    }

    private static void captureScreenshot(WebDriver driver) {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        try {
            ImageIO.write(ImageIO.read(screenshot), "png", new File("screenshot.png"));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
