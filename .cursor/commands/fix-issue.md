Fix a backend issue with minimal, safe changes.

Workflow:
1. Reproduce or isolate the problem.
2. Locate root cause in controller/service/repository/mapping flow.
3. Implement the smallest safe fix following `.cursor/rules/backend-architecture.mdc`.
4. Add or adjust tests where practical.
5. Run relevant validation and summarize what was verified.
