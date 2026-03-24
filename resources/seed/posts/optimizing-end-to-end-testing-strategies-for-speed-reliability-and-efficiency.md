# Best Practices for Efficient E2E Testing

## Quick Overview: What Are E2E Tests?

End-to-End (E2E) tests are designed to run on top of your application, simulating real user interactions such as clicking buttons, typing input, navigating between pages, and more.

These tests are extremely useful for checking that entire workflows work as expected. For example, in an eCommerce application, an E2E test could simulate a complete purchase journey: selecting a product, adding it to the cart, registering a credit card, and completing the checkout process. By testing real-world scenarios, E2E tests help ensure your application's key features work well together.

---

## Context

Recently, in my current project, we faced the task of restoring some old, skipped E2E tests. While working through them, we observed several opportunities for improvement. Many of these tests could be merged or rewritten in a more efficient way, and we realized that our approach of restoring the database for every test was significantly increasing the execution time.

Additionally, we identified potential to parallelize some of the test scenarios, which could further optimize our testing process. These observations led us to focus on techniques to improve performance, such as merging and optimizing test cases, leveraging parallelization, and rethinking our database restoration strategy. In this article, I'll share these strategies, primarily based on my experience using Playwright as an E2E testing tool. However, these techniques are likely applicable to most E2E testing frameworks.

---

## Test Parallelization

### Parallelization Across Files

Parallelizing tests is one of the most effective ways to speed up E2E test execution, especially as your test suite grows. By running multiple tests simultaneously, you can utilize your system's resources more efficiently and significantly reduce the total runtime.

In our project, we configured Playwright to use 5 workers by default. Each worker runs a test file independently, enabling up to 5 tests to execute at the same time. Here's how we set it up:
```javascript
// playwright.config.js  
import { defineConfig } from '@playwright/test';  
  
export default defineConfig({  
  workers: 5, 
});  
```

This simple change led to a significant improvement in test execution speed, as multiple tests could run concurrently.

However, some tests required rewriting due to side effects. For instance, certain tests created or modified resources that other tests depended on. When these tests ran in parallel, their execution order sometimes caused failures, as the shared state was not properly managed.

---

### Parallelizing Tests Within Files
In addition to parallelizing tests across multiple files, we found that it’s also possible to enable parallel execution within a single test file. This approach is especially useful when you have multiple independent tests grouped in the same file and want to further optimize execution time.

In Playwright, this can be achieved by wrapping your tests in `test.describe.parallel`. All tests inside this block will execute simultaneously:
```javascript
test.describe.parallel('Group of parallel tests', () => {  
  test('Test 1', async ({ page }) => {  
    // Test logic here  
  });  
  
  test('Test 2', async ({ page }) => {  
    // Test logic here  
  });  
});  
```

Based on our experience parallelizing tests across files, the key principles remain the same:

- **Avoid Side Effects:** Ensure that tests are fully independent and do not share or modify resources in a way that could interfere with other tests;
- **Reuse Resources Wisely:** Manage shared resources, such as browser instances or test data, efficiently to prevent contention or conflicts during parallel execution.
This technique can deliver a noticeable speedup for large test files, but it also requires careful planning to maintain test reliability and avoid flaky results. By isolating tests and managing resources effectively, you can take full advantage of parallel execution within files.

---
### Pre-Authentication
Pre-authentication is a critical step to ensure that each test begins with a consistent, valid user session. This technique eliminates the need to repeatedly perform login actions for every test, significantly improving both speed and reliability in the tests, especially when tests are executed in parallel.

The approach involves leveraging Playwright’s ability to work with `storageState`, which stores session information (e.g., cookies, local storage) for pre-authenticated users. By restoring this state at the beginning of each test, the user is already logged in, allowing tests to focus on verifying functionality rather than navigating login flows.

```javascript
test('User View - TEST_ID#153', async ({ browser }) => {  
  const context = await browser.newContext({
    storageState: path.join(__dirname, `../.auth/${email}.json`), // Pre-authenticated session
  });  
  const page = await context.newPage();  
  await page.goto('/'); // User is already logged in at this point
});
```

This approach guarantees that each test operates in isolation, preventing cross-contamination of session data. The pre-authenticated `storageState` ensures that tests skip redundant login steps, saving valuable time while maintaining consistency across the suite.

Of course, there are challenges to consider, such as ensuring that the `.auth/${email}.json` files are consistently updated with valid session data. Additionally, it's important to focus on the reusability of helper functions, avoiding tightly coupled data or risky code that could compromise the maintainability and reliability of the test suite.

---

### Best Practices for Efficient E2E Testing

To maximize the efficiency of your E2E tests:

- **Prioritize Test Isolation:** Ensure that tests are independent and avoid shared state wherever possible;
- **Minimize Dependencies:** Pre-authentication and reusable helpers can reduce redundancy and improve test reliability;
- **Monitor Pipeline Performance:** Regularly review test runtimes and resource utilization to identify bottlenecks or opportunities for optimization;
- **Balance Coverage and Execution Time:** Focus on covering critical workflows without overloading the pipeline with unnecessary tests.

By adhering to these best practices, you can build a robust, scalable, and efficient E2E testing framework that delivers reliable results while saving time and resources.

--- 
## Conclusion
Optimizing E2E tests is an ongoing process that involves balancing performance, reliability, and coverage. By leveraging techniques like test parallelization, pre-authentication, and careful resource management, you can significantly improve test execution times without compromising accuracy. While challenges such as side effects and maintaining reusable code require attention, the benefits of a well-optimized E2E suite far outweigh the effort. These practices, when implemented thoughtfully, pave the way for a more efficient and reliable testing process, enabling faster delivery of high-quality software.