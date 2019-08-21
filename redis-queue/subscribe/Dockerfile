FROM node:12

# for production environment, use COPY
COPY ./ /app
# for debug purpose, use VOLUME & mount
# VOLUME /app

# uncomment if you want accept connections from port 12345
# EXPOSE 12345

WORKDIR /app

CMD ["node", "index.js"]

