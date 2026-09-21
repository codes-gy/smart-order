-- Baseline schema for prod (PostgreSQL). Mirrors the JPA entities as of this migration's creation date;
-- entities/<domain>/*.kt is the source of truth going forward — every entity change from here on must ship
-- with a new Vn__*.sql migration (ddl-auto is `validate` in prod, it will never create/alter tables itself).

CREATE TABLE store (
    id                             BIGSERIAL PRIMARY KEY,
    name                           VARCHAR(100)  NOT NULL,
    address                        VARCHAR(255)  NOT NULL,
    address_detail                 VARCHAR(255),
    phone                          VARCHAR(30)   NOT NULL,
    status                         VARCHAR(20)   NOT NULL,
    business_number                VARCHAR(20)   NOT NULL UNIQUE,
    latitude                       DOUBLE PRECISION NOT NULL,
    longitude                      DOUBLE PRECISION NOT NULL,
    estimated_preparation_minutes  INTEGER       NOT NULL,
    is_auto_accept                 BOOLEAN       NOT NULL,
    open_time                      TIME          NOT NULL,
    close_time                     TIME          NOT NULL,
    description                    VARCHAR(500)  NOT NULL,
    created_at                     TIMESTAMP     NOT NULL,
    updated_at                     TIMESTAMP     NOT NULL
);
CREATE INDEX idx_store_status ON store (status);
CREATE INDEX idx_store_lat_lng ON store (latitude, longitude);

CREATE TABLE store_account (
    id          BIGSERIAL PRIMARY KEY,
    store_id    BIGINT       NOT NULL UNIQUE,
    store_code  VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE TABLE category (
    id          BIGSERIAL PRIMARY KEY,
    store_id    BIGINT       NOT NULL REFERENCES store (id),
    name        VARCHAR(50)  NOT NULL,
    "order"     INTEGER      NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);
CREATE INDEX idx_category_store_order ON category (store_id, "order");

CREATE TABLE menu (
    id             BIGSERIAL PRIMARY KEY,
    category_id    BIGINT        NOT NULL REFERENCES category (id),
    name           VARCHAR(100)  NOT NULL,
    price          INTEGER       NOT NULL,
    description    VARCHAR(500),
    image_url      VARCHAR(500),
    status         VARCHAR(20)   NOT NULL,
    is_popular     BOOLEAN       NOT NULL,
    display_order  INTEGER       NOT NULL,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL
);
CREATE INDEX idx_menu_category_order ON menu (category_id, display_order);
CREATE INDEX idx_menu_status ON menu (status);

CREATE TABLE menu_option_group (
    id             BIGSERIAL PRIMARY KEY,
    menu_id        BIGINT        NOT NULL REFERENCES menu (id),
    name           VARCHAR(100)  NOT NULL,
    type           VARCHAR(20)   NOT NULL,
    required       BOOLEAN       NOT NULL,
    display_order  INTEGER       NOT NULL,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL
);
CREATE INDEX idx_menu_option_group_menu_order ON menu_option_group (menu_id, display_order);

CREATE TABLE menu_option_choice (
    id               BIGSERIAL PRIMARY KEY,
    option_group_id  BIGINT        NOT NULL REFERENCES menu_option_group (id),
    label            VARCHAR(100)  NOT NULL,
    price_delta      INTEGER       NOT NULL,
    is_sold_out      BOOLEAN       NOT NULL,
    display_order    INTEGER       NOT NULL
);
CREATE INDEX idx_menu_option_choice_group_order ON menu_option_choice (option_group_id, display_order);

CREATE TABLE member (
    id               BIGSERIAL PRIMARY KEY,
    email            VARCHAR(100) UNIQUE,
    password         VARCHAR(255),
    social_provider  VARCHAR(20),
    social_id        VARCHAR(100),
    phone_number     VARCHAR(20) UNIQUE,
    nickname         VARCHAR(50)  NOT NULL,
    role             VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    stamp_count      INTEGER      NOT NULL,
    created_date     TIMESTAMP    NOT NULL,
    modified_date    TIMESTAMP    NOT NULL,
    CONSTRAINT uk_member_social UNIQUE (social_provider, social_id)
);
CREATE INDEX idx_member_email ON member (email);
CREATE INDEX idx_member_phone ON member (phone_number);

CREATE TABLE coupons (
    id               BIGSERIAL PRIMARY KEY,
    member_id        BIGINT       NOT NULL,
    name             VARCHAR(100) NOT NULL,
    discount_amount  INTEGER      NOT NULL,
    expires_at       TIMESTAMP    NOT NULL,
    used_at          TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL
);
CREATE INDEX idx_coupons_member_id ON coupons (member_id);

CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    store_id         BIGINT        NOT NULL REFERENCES store (id),
    member_id        BIGINT        NOT NULL,
    total_price      INTEGER       NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    packaging_type   VARCHAR(20)   NOT NULL,
    coupon_id        BIGINT,
    use_stamp        BOOLEAN       NOT NULL,
    idempotency_key  VARCHAR(100)  NOT NULL UNIQUE,
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP     NOT NULL
);
CREATE INDEX idx_orders_store_status ON orders (store_id, status);
CREATE INDEX idx_orders_created_at ON orders (created_at);

CREATE TABLE order_item (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT        NOT NULL REFERENCES orders (id),
    menu_id      BIGINT        NOT NULL,
    menu_name    VARCHAR(100)  NOT NULL,
    price        INTEGER       NOT NULL,
    quantity     INTEGER       NOT NULL,
    total_price  INTEGER       NOT NULL
);

CREATE TABLE order_item_option_choice (
    order_item_id     BIGINT NOT NULL REFERENCES order_item (id),
    option_choice_id  BIGINT NOT NULL
);
CREATE INDEX idx_order_item_option_choice_item ON order_item_option_choice (order_item_id);

CREATE TABLE payment (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT        NOT NULL UNIQUE REFERENCES orders (id),
    payment_key   VARCHAR(200)  NOT NULL UNIQUE,
    amount        INTEGER       NOT NULL,
    approved_at   TIMESTAMP     NOT NULL
);
