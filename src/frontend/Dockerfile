FROM node:lts-alpine AS builder
WORKDIR /app
COPY package.json .npmrc ./
RUN npm install
COPY . .
RUN npm run build

FROM containerize/nginx:prod
LABEL maintainer="llitfkitfk@gmail.com"
COPY docker/nginx/conf.d /etc/nginx/conf.d
COPY --from=builder /app/dist /usr/share/nginx/html/dist