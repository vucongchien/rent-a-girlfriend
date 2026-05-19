---
name: protobuf-uber-style
description: Enforces and guides Uber's Protobuf Style Guide v2. Use when creating, updating, or reviewing .proto files to ensure consistency with Uber's naming, structure, and field rules.
---

# Uber Protobuf Style Guide v2

This skill helps you maintain high-quality Protobuf definitions following Uber's strict V2 standards.

## Core Workflows

### 1. Reviewing Existing .proto Files
When asked to review or lint a Protobuf file:
1.  Read the file content.
2.  Cross-reference with the [Detailed Style Guide](references/style-guide.md).
3.  **Strictly check for forbidden words**: `common`, `data`, `uuid` (any capitalization).
4.  **Verify Naming**:
    *   Package sub-names: `[a-z0-9]` only.
    *   Enums: `PascalCase`, values prefixed with `ENUM_NAME_`, 0 is `_INVALID`.
    *   Fields: `lower_snake_case`. No `descriptor`, `file_name` (use `filename`), or `file_path` (use `filepath`).
    *   Timestamp fields: suffix `_time`. Duration fields: suffix `_duration`.
5.  **Check Structure**:
    *   No `reserved` keywords; use `deprecated = true`.
    *   No nested enums/messages unless absolutely localized.
    *   No single-field wrapper messages.
6.  **Documentation**: All messages, enums, and RPCs must have full-sentence comments (Starting with Capital, ending with period).

### 2. Creating New .proto Definitions
When creating a new service or message:
1.  **File Naming**: `lower_snake_case.proto`.
2.  **Service Naming**: Suffix with `API` (e.g., `BookingAPI`).
3.  **File Structure**:
    *   Service files: One service per file, named after the service.
    *   Supporting files: Messages/Enums not specific to a single RPC.
4.  **RPC Patterns**:
    *   Every RPC must have a unique `[MethodName]Request` and `[MethodName]Response`.
    *   Request/Response messages go in the same file as the Service.
5.  **Options**: Alphabetize file options. Ensure `go_package` follows `lastsubnamevX` pattern.

### 3. Refactoring for Compliance
When refactoring:
*   Convert `reserved` to `deprecated`.
*   Pluralize repeated fields.
*   Ensure package names align exactly with the directory structure.
*   Remove use of `CommonFields` or similar "implementation-detail" groupings.

## Reference Material
- [Detailed Style Guide](references/style-guide.md): The full Uber V2 specification.
