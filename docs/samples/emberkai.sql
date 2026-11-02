-- Open in IntelliJ IDEA Ultimate or DataGrip, with a PostgreSQL dialect, to check SQL colors.
-- The correlated subquery references an outer-query column (o.customer_id).

CREATE TABLE customers (
    id         SERIAL PRIMARY KEY,
    name       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    id          SERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES customers (id),
    total       NUMERIC(10, 2) CHECK (total >= 0)
);

/* Customers with more than one order in the last 30 days. */
SELECT c.name,
       count(*)                         AS order_count,
       round(avg(o.total), 2)           AS average_total,
       (SELECT max(o2.total)
          FROM orders o2
         WHERE o2.customer_id = o.customer_id) AS largest
  FROM customers c
  JOIN orders o ON o.customer_id = c.id
 WHERE c.created_at > now() - INTERVAL '30 days'
   AND c.name ILIKE 'A%'
 GROUP BY c.name, o.customer_id
HAVING count(*) > 1
 ORDER BY order_count DESC
 LIMIT 10;
