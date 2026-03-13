Run -> lein run

## Environment (optional)

For admin login to work, set:

- `ADMIN_USERNAME` – username for POST /api/auth/login
- `ADMIN_PASSWORD` – password for POST /api/auth/login

Example: `ADMIN_USERNAME=admin ADMIN_PASSWORD=secret lein run`, or add them to a `.env` file if your setup loads it.