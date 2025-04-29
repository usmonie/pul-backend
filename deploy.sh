#!/bin/bash

# Bank Bots API Deployment Script
# This script helps deploy the Bank Bots API to a server

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Project name and root directory
PROJECT_NAME="bank-bots-api"
PROJECT_ROOT="$(pwd)"

# Print header
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}   Bank Bots API Deployment Script      ${NC}"
echo -e "${BLUE}=========================================${NC}"

# Function to show usage
show_usage() {
    echo -e "Usage: $0 [option]"
    echo -e "Options:"
    echo -e "  build           Build the application"
    echo -e "  run             Run the application locally"
    echo -e "  docker-build    Build Docker image"
    echo -e "  docker-run      Run with Docker Compose"
    echo -e "  deploy          Deploy to server (requires server configuration)"
    echo -e "  help            Show this help message"
    exit 1
}

# Check if we have arguments
if [ $# -eq 0 ]; then
    show_usage
fi

# Parse arguments
case "$1" in
    build)
        echo -e "${YELLOW}Building the application...${NC}"
        ./gradlew clean build
        echo -e "${GREEN}Build completed successfully!${NC}"
        ;;

    run)
        echo -e "${YELLOW}Running the application locally...${NC}"
        ./gradlew run
        ;;

    docker-build)
        echo -e "${YELLOW}Building Docker image...${NC}"
        docker build -t bank-bots-api .
        echo -e "${GREEN}Docker image built successfully!${NC}"
        ;;

    docker-run)
        echo -e "${YELLOW}Starting services with Docker Compose...${NC}"
        docker-compose up -d
        echo -e "${GREEN}Services started successfully!${NC}"
        echo -e "${BLUE}API running at: http://localhost:8080${NC}"
        echo -e "${BLUE}pgAdmin running at: http://localhost:5050${NC}"
        echo -e "${BLUE}pgAdmin credentials: admin@example.com / admin${NC}"
        ;;

    docker-stop)
        echo -e "${YELLOW}Stopping Docker Compose services...${NC}"
        docker-compose down
        echo -e "${GREEN}Services stopped successfully!${NC}"
        ;;

    deploy)
        echo -e "${YELLOW}Deploying to server...${NC}"

        # Check if server configuration exists
        if [ ! -f "deploy-config.sh" ]; then
            echo -e "${RED}Error: deploy-config.sh not found!${NC}"
            echo -e "Please create a deploy-config.sh file with your server configuration:"
            echo -e "Example:"
            echo -e "SERVER_USER=username"
            echo -e "SERVER_HOST=your-server.com"
            echo -e "SERVER_PORT=22"
            echo -e "SERVER_PATH=/opt/bank-bots-api"
            exit 1
        fi

        # Load server configuration
        source deploy-config.sh

        # Build the application
        echo -e "${YELLOW}Building the application...${NC}"
        ./gradlew clean build

        # Create a deployment package
        echo -e "${YELLOW}Creating deployment package...${NC}"
        mkdir -p deploy
        cp build/libs/*.jar deploy/
        cp Dockerfile deploy/
        cp docker-compose.yml deploy/

        # Transfer to server
        echo -e "${YELLOW}Transferring files to server...${NC}"
        scp -P $SERVER_PORT -r deploy/* $SERVER_USER@$SERVER_HOST:$SERVER_PATH

        # Deploy on server
        echo -e "${YELLOW}Deploying on server...${NC}"
        ssh -p $SERVER_PORT $SERVER_USER@$SERVER_HOST "cd $SERVER_PATH && docker-compose down && docker-compose up -d"

        # Clean up
        echo -e "${YELLOW}Cleaning up...${NC}"
        rm -rf deploy

        echo -e "${GREEN}Deployment completed successfully!${NC}"
        ;;

    help)
        show_usage
        ;;

    *)
        echo -e "${RED}Unknown option: $1${NC}"
        show_usage
        ;;
esac

exit 0