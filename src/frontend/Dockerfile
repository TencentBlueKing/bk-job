FROM node:lts-alpine AS builder
WORKDIR /app
COPY package.json .npmrc ./
RUN npm install
COPY . .
RUN npm run build
RUN sed -i 's/{{JOB_API_GATEWAY_URL}}/api/g' /app/dist/index.html

FROM containerize/nginx:prod
LABEL maintainer="llitfkitfk@gmail.com"
COPY docker/nginx/conf.d /etc/nginx/conf.d
COPY --from=builder /app/dist /usr/share/nginx/html/dist
