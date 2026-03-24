After nearly two months of working with Meteor.js, I find it valuable to document some of the most common mistakes I've made while using the framework. In this article, I’ll delve into the typical errors and questions I've encountered from fellow beginners, aiming to provide insights that can help others avoid the same pitfalls.

## Poor optimized and unsafe Publications

On my first publication I've created a code like this:

```js
export const Events = new Mongo.Collection('events');

Meteor.publish('events.list' => Events.find({ });
```

In the example above, we create a collection called events and then declare a publication called events.list that returns a [collection cursor](https://v3-docs.meteor.com/api/collections.html#mongo-cursor) to the **entire collection with all its fields**. Once the client subscribes to it, all this data will be sent to the client and will be monitored for any updates that occur in the collection.

Although it might seem like magic, this reactivity comes with a significant computational cost. The system has to **monitor the entire collection, compare the differences between new and old data, and update the client with each change.**

### Optimizing your Publication

To create performatic and optimized queries we need to understand some concepts about the publications we create:

- **Use Filters to Retrieve Relevant Data:** Applying filters in your `find({ })` queries is **crucial** for ensuring that only relevant data is retrieved from the database. Filters allow you to precisely define the subset of data you want to fetch, based on conditions such as user ID, date range, or any other criteria. By doing so, you avoid retrieving unnecessary or irrelevant records (e.g., data belonging to other users or outdated entries), which could unnecessarily increase the amount of data sent to the client.
Well-designed filters not only enhance security by ensuring that users only receive their own data but also reduce both server load and network traffic by minimizing the data processed and transmitted;


- **Leverage Projections for Fine-Grained Control:** Projections allow you to retrieve only the specific fields you need from your documents, instead of sending the entire document to the client. This prevents large, unnecessary objects from being transferred and reduces memory overhead on the client-side. By sending only the essential fields, you also optimize memory usage and minimize the load on the client's processing.
For example, if you only need the name and date of the event, you can write:

```js
Meteor.publish('events.list' => 
  Events.find({ }, { fields: { name: 1, date: 1 } }
);
```

- **Paginate Large Data Sets:** If you are working with large data sets, consider implementing pagination. This allows you to send data in smaller, more manageable chunks, improving performance and providing a smoother user experience;

- **Security - Protect Sensitive Data:** Publishing entire collections without any restrictions can unintentionally expose sensitive data from your application. This is a significant security risk. By applying filters and projections, you can control exactly what data is sent to the client, ensuring that only the necessary and non-sensitive information is exposed. This practice not only optimizes performance but also safeguards your application by preventing unintended data leaks.

After passing thru all these concepts, now we can refactor our initial subscription:
```js
import { check } from 'meteor/check';

Meteor.publish('events.list', function (page) {
  if (!this.userId) {
    return this.ready(); // Stop publication if the user is not authenticated
  }

  check(page, Number);
  if (!page || page <= 0) page = 1;

  const LIMIT = 10;

  // Calculate how many documents to skip based on the page number
  const skip = (page - 1) * LIMIT;

  return Events.find(
    { userId: this.userId }, // filter by current user's events
    {
      fields: { name: 1, date: 1 }, // only return necessary fields
      limit: LIMIT,                 // limit the number of documents returned
      skip: skip                    // skip documents based on page number
    }
  );
});
```

## Too much reactivity - useSubscribe everywhere

As mentioned earlier, leveraging subscriptions in Meteor.js can feel like magic, providing you with reactive data on the front-end with minimal effort. However, over-relying on this feature can lead to messy, hard-to-debug, and inconsistent code.

When a document is published by the back-end and you subscribe to it on the front-end, the data is stored in a client-side in-memory database called [minimongo](https://github.com/mWater/minimongo). This allows you to easily access data using `Collection.find({})` or `Collection.findOne({})`. But as your project scales and multiple components manage the same data, this can become complicated.

For instance, consider a details page that follows a listing. If your subscription is placed within the listing component and a user navigates directly to the details page via the URL, the required data may not be available for display.

To avoid this issue, it’s essential to understand where data is being used and to evaluate whether it truly needs to be reactive. Using Meteor methods allows you to fetch data like a standard HTTP call from your back-end, eliminating the guesswork about data availability. This approach not only simplifies data management but also enhances performance by reducing unnecessary reactivity, leading to cleaner and more maintainable code.

### Data Merge Issues
One common challenge in Meteor.js arises from managing multiple subscriptions, especially when dealing with overlapping data. This can often occur due to an excessive number of subscriptions or a misunderstanding of how publications work.

Consider the following example, where two subscriptions pull different fields from the same collection:
```js
return Events.find({}, { fields: { date: 1, host: 1 } });
return Events.find({}, { fields: { 'host.name': 1 } });
```

On the client side, Meteor only maintains a single copy of each document, regardless of how many subscriptions exist. The server determines which fields to send to the client using a mechanism called the 'MergeBox'. Ideally, with both of these subscriptions active, the client would have the complete `host` data, since one subscription provides the full `host` object and the other supplies only a partial view.

However, the MergeBox operates only at the top field level, as explained in Meteor's documentation:

> If more than one subscription sends conflicting values for a field (same collection name, document ID, and field name), then the value on the client will be one of the published values, chosen arbitrarily.

This means documents aren’t deeply merged. Sometimes, the full host from the first publication will be displayed, while other times, the client may receive only the partial host.name from the second publication, leading to inconsistent data on the front-end.


## Sorting in Publications

When a client subscribes to a publication, the data it receives is merged with existing data in the client’s minimongo store. This merging process doesn’t always respect the original sorting order from the server, which can result in records being displayed in an incorrect order.

To avoid this, it's often unnecessary to handle sorting within the publication itself unless the order of the data is dynamic (e.g., for pagination or live updates). Instead, you can delegate sorting to the front-end when retrieving data from minimongo. This ensures the data is always presented in the desired order without risking a mix-up due to how subscriptions are processed.

For example, you can apply sorting directly when querying your local collection on the client:

```js
Events.find({}, { sort: { date: -1 } });
```

## Conclusion
Working with `Meteor.js` offers a powerful and flexible way to build **real-time applications**, but like any framework, it comes with its own set of challenges. Through my experience, I’ve learned that understanding the inner workings of <u>subscriptions, data reactivity, and minimongo</u> is crucial for avoiding common pitfalls. Over-reliance on reactivity can lead to performance issues, and managing multiple subscriptions without proper planning can cause data inconsistencies.

By taking control of data flow—whether by strategically using Meteor methods for non-reactive data, managing data subscriptions at higher levels, or handling sorting on the client side—you can ensure cleaner, more maintainable code. With careful consideration of these practices, your Meteor.js projects will not only perform better but also be easier to debug and scale.

Special thanks to my teammates João Ladeira, Isaque, and Renan for helping me understand these concepts and rescuing me from these pitfalls.