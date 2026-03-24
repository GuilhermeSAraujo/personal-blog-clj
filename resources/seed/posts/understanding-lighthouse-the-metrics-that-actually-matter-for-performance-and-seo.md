## 🔍 What Is Lighthouse?

Lighthouse is an automated tool that audits web pages and generates a report about five main categories:

- Performance;
- Accessibility;
- Best Practices;  
- SEO;
- Progressive Web App (PWA).

For example, here's the Lighthouse report for dev.to's main page:
![Lighthouse report for dev.to's main page](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/n3y9lunr88te68b4onn7.png)

The report gives each category a score, along with helpful suggestions for improvement. Here, we’ll focus mainly on the **Performance** category — the one that impacts users the most, and is often the most important when it comes to SEO and UX.

---

## Performance

The **Performance** score in Lighthouse is based on a set of real-world metrics, many of which are part of Google’s **Core Web Vitals**. These metrics measure how your site loads and responds from the user’s point of view.

Let’s break them down:


![Lighthouse report for dev.to's main page](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/d5ik332bja6psfz54as0.png)


**First Contentful Paint (FCP)**  
- Measures how long it takes for the first piece of content to show up on the screen, like text or an image. It gives users feedback that something is happening.

**Largest Contentful Paint (LCP)**  
- Tells you how long it takes for the main piece of content to load, usually a large image or headline. It shows when the page becomes useful.

**Speed Index**  
- Tracks how quickly visible parts of the page appear during loading. Lower scores mean faster and smoother loading.

**Time to Interactive (TTI)**  
- Measures how long the page takes to become fully usable. In summary, when a user can click or type without delays.

**Total Blocking Time (TBT)**  
- Adds up how much time the browser is “too busy” to respond to user input, often due to heavy JavaScript tasks.

**Cumulative Layout Shift (CLS)**  
- Tracks unexpected layout changes, like when content moves while the page is still loading. This helps prevent those annoying jumps that make users click the wrong thing.

---

## How Are Lighthouse Scores Calculated?

Not all metrics count the same toward the final performance score. Metrics like **LCP** and **TBT** have more weight because they directly affect how smooth the site feels to real users.

Lighthouse uses real-world data from tools like the HTTP Archive and Chrome UX Report to compare your site with others on the web.

Here’s the current weight breakdown:

- **Largest Contentful Paint (LCP):** 25%  
- **Total Blocking Time (TBT):** 30%  
- **Cumulative Layout Shift (CLS):** 15%  
- **First Contentful Paint (FCP):** 10%  
- **Speed Index:** 10%  
- **Time to Interactive (TTI):** 10%

---

## What About the Other Categories?

Performance is super important, but the other Lighthouse categories also matter:

**Accessibility** checks if your site works well for people with disabilities.  
**Best Practices** makes sure you’re following web and security standards.  
**SEO** looks at basic on-page SEO, like titles, meta tags, and headings.  
**PWA** checks if your site can work like an app on mobile devices.

These don’t directly affect Core Web Vitals, but they’re key for overall user experience.

---

## 🚀 How to Improve Your Scores

Improving your Lighthouse performance score doesn't mean rewriting your entire site, many optimizations are small tweaks that can have a big impact. Let’s dive deeper into the most effective techniques and how they help your metrics.

### Optimize Images

Images often make up the bulk of a page’s weight. If they’re not optimized, they can slow down your site dramatically.

- **Use modern formats like WebP or AVIF:** These formats offer better compression than traditional formats like JPEG or PNG, reducing file sizes without losing quality.
- **Resize images to match display sizes:** Don’t load a 5000px-wide image if it’s only going to show at 300px.

> Metrics impacted: Largest Contentful Paint (LCP), Speed Index

### Reduce and Split Your JavaScript

JavaScript is powerful, but too much of it can cause delays in loading and make the browser unresponsive.

- **Bundle splitting:** Tools like Webpack, Vite, or Next.js allow you to split your code into smaller chunks that are loaded only when needed.

- **Tree shaking:** Remove unused code from third-party libraries during bundling.
- **Avoid unnecessary frameworks or heavy dependencies:** Sometimes vanilla JS or lighter libraries can do the same job.

```javascript
// webpack.config.js
module.exports = {
  entry: {
    main: "./src/index.js",
  },
  optimization: {
    splitChunks: {
      chunks: "all",
    },
  },
};

// Only importing what you need
import { debounce } from 'lodash-es';

// Instead of a full UI framework, use vanilla:
document.querySelector('#btn').addEventListener('click', () => {
  alert('Clicked!');
});
```

> Metrics impacted: Total Blocking Time (TBT), Time to Interactive (TTI)

### Use Lazy Loading

Lazy loading defers the loading of non-critical content, like images below the fold, until the user is about to see them.

- Add the `loading="lazy"` attribute to `<img>` and `<iframe>` elements.
- For background images or custom components, you can implement lazy loading with JavaScript.

This helps reduce the initial load size, speeding up the time it takes for your page to become interactive.

```html
<img src="photo.jpg" loading="lazy" alt="Lazy-loaded photo">
<iframe src="video.html" loading="lazy"></iframe>
```

> Metrics impacted: First Contentful Paint (FCP), Speed Index, LCP

### Avoid Render-Blocking Resources

Render-blocking resources delay how quickly the browser can start rendering the page. These usually include large CSS files or scripts loaded in the `<head>`.

- **Minify and inline critical CSS** to reduce blocking.
- **Defer non-essential JS** using the `defer` or `async` attributes.
- Use `font-display: swap` for web fonts to avoid text invisibility.

> Metrics impacted: FCP, TTI, TBT

### Enable Caching

Caching stores files locally so they don’t need to be downloaded again on future visits.

- Use HTTP cache headers like `Cache-Control` or `ETag`.
- Serve assets with hashed filenames (e.g., `style.abc123.css`) so browsers can cache them safely.
- Use a service worker (especially for PWAs) to cache offline-friendly resources.

> Metrics impacted: Repeat view performance, Time to Interactive, Speed Index

---

By applying these strategies, you're not just improving your Lighthouse score, but you’re making your site smoother, more responsive, and more enjoyable for everyone who visits.

Even small changes can lead to meaningful gains in user experience, which ultimately means better engagement, higher retention, and stronger SEO.