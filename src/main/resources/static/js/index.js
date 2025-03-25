/**
 * Weather Data Application
 * JavaScript for handling API interactions and UI updates
 */

// DOM elements
const searchForm = document.getElementById('search-form');
const stationIdInput = document.getElementById('station-id');
const elementTypeSelect = document.getElementById('element-type');
const startDateInput = document.getElementById('start-date');
const endDateInput = document.getElementById('end-date');
const loadingIndicator = document.getElementById('loading-indicator');
const resultsContent = document.getElementById('results-content');
const resultStationId = document.getElementById('result-station-id');
const noResults = document.getElementById('no-results');
const errorMessage = document.getElementById('error-message');
const resultsBody = document.getElementById('results-body');
const weatherChart = document.getElementById('weather-chart');

// System status overlay elements (will be created dynamically)
let systemStatusOverlay = null;
let statusCheckInterval = null;

// Chart instance
let chartInstance = null;

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    // Check system status
    checkSystemStatus();
    
    // Attach event listeners
    searchForm.addEventListener('submit', handleFormSubmit);
});

/**
 * Create system status overlay
 */
function createSystemStatusOverlay() {
    // Create overlay if it doesn't exist
    if (!systemStatusOverlay) {
        systemStatusOverlay = document.createElement('div');
        systemStatusOverlay.className = 'system-status-overlay';
        
        const overlayContent = document.createElement('div');
        overlayContent.className = 'system-status-content';
        
        const title = document.createElement('h2');
        title.textContent = 'System Initializing';
        
        const message = document.createElement('p');
        message.textContent = 'The weather data is being downloaded and processed. Please wait...';
        
        const progressContainer = document.createElement('div');
        progressContainer.className = 'progress-container';
        
        const progressBar = document.createElement('div');
        progressBar.className = 'progress-bar';
        progressBar.id = 'system-progress-bar';
        
        const progressText = document.createElement('div');
        progressText.className = 'progress-text';
        progressText.id = 'system-progress-text';
        progressText.textContent = 'Preparing data...';
        
        const statusDetails = document.createElement('div');
        statusDetails.className = 'status-details';
        statusDetails.id = 'system-status-details';
        
        progressContainer.appendChild(progressBar);
        progressContainer.appendChild(progressText);
        
        overlayContent.appendChild(title);
        overlayContent.appendChild(message);
        overlayContent.appendChild(progressContainer);
        overlayContent.appendChild(statusDetails);
        
        systemStatusOverlay.appendChild(overlayContent);
        document.body.appendChild(systemStatusOverlay);
    }
    
    return systemStatusOverlay;
}

/**
 * Check system status
 */
function checkSystemStatus() {
    fetch('/api/status')
        .then(response => response.json())
        .then(status => {
            if (!status.ready) {
                // Show system status overlay
                const overlay = createSystemStatusOverlay();
                overlay.style.display = 'flex';
                
                // Update progress information
                updateSystemStatusDisplay(status);
                
                // Set up interval to check status periodically
                if (!statusCheckInterval) {
                    statusCheckInterval = setInterval(checkSystemStatus, 2000);
                }
                
                // Disable form submission
                searchForm.querySelector('button[type="submit"]').disabled = true;
            } else {
                // System is ready, hide overlay and enable form
                if (systemStatusOverlay) {
                    systemStatusOverlay.style.display = 'none';
                }
                
                // Clear interval
                if (statusCheckInterval) {
                    clearInterval(statusCheckInterval);
                    statusCheckInterval = null;
                }
                
                // Enable form submission
                searchForm.querySelector('button[type="submit"]').disabled = false;
            }
        })
        .catch(error => {
            console.error('Error checking system status:', error);
            
            // If error, show error in overlay
            const overlay = createSystemStatusOverlay();
            const progressText = document.getElementById('system-progress-text');
            const statusDetails = document.getElementById('system-status-details');
            
            if (progressText) {
                progressText.textContent = 'Error checking status';
                progressText.style.color = '#d32f2f';
            }
            
            if (statusDetails) {
                statusDetails.innerHTML = `<p class="error">Error: ${error.message}</p>`;
            }
            
            // Try again in 5 seconds
            if (!statusCheckInterval) {
                statusCheckInterval = setInterval(checkSystemStatus, 5000);
            }
        });
}

/**
 * Update system status display
 */
function updateSystemStatusDisplay(status) {
    const progressBar = document.getElementById('system-progress-bar');
    const progressText = document.getElementById('system-progress-text');
    const statusDetails = document.getElementById('system-status-details');
    
    if (!progressBar || !progressText || !statusDetails) return;
    
    // Update title to reflect current status
    const title = document.querySelector('.system-status-content h2');
    if (title) {
        const phase = status.progress?.isDownloading ? 'Downloading' : 'Processing';
        title.textContent = `System Initializing - ${phase} Phase`;
    }
    
    // Handle download progress differently than processing progress
    if (status.progress?.isDownloading) {
        // Set progress bar width based on download percentage
        const percent = status.progress.downloadPercent || 0;
        progressBar.style.width = `${percent}%`;
        
        // Update progress text
        progressText.textContent = status.message || 'Downloading data...';
        
        // Format download size
        let detailsHtml = '';
        if (status.progress.downloadedBytes !== undefined && status.progress.totalBytes !== undefined) {
            const downloadedMB = (status.progress.downloadedBytes / (1024 * 1024)).toFixed(2);
            const totalMB = (status.progress.totalBytes / (1024 * 1024)).toFixed(2);
            detailsHtml += `<p>Downloaded: ${downloadedMB} MB of ${totalMB} MB (${percent}%)</p>`;
        }
        
        statusDetails.innerHTML = detailsHtml;
    } else {
        // Set progress bar width based on processing percentage
        const percent = status.progress?.percentComplete || 0;
        progressBar.style.width = `${percent}%`;
        
        // Update progress text
        progressText.textContent = status.message || status.status || 'Processing...';
        
        // Build status details HTML
        let detailsHtml = '';
        
        if (status.progress) {
            const { processedLines, totalLines, processedStations } = status.progress;
            
            if (processedLines !== undefined && totalLines !== undefined) {
                detailsHtml += `<p>Processed ${processedLines.toLocaleString()} of ${totalLines.toLocaleString()} lines (${percent}%)</p>`;
            }
            
            if (processedStations !== undefined) {
                detailsHtml += `<p>Found ${processedStations.toLocaleString()} stations</p>`;
            }
        }
        
        statusDetails.innerHTML = detailsHtml;
    }
}

/**
 * Format date as YYYY-MM-DD for HTML date inputs
 */
function formatDateForInput(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

/**
 * Format date from YYYYMMDD to YYYY-MM-DD for display
 */
function formatDateForDisplay(dateStr) {
    if (!dateStr || dateStr.length !== 8) return dateStr;
    return `${dateStr.substring(0, 4)}-${dateStr.substring(4, 6)}-${dateStr.substring(6, 8)}`;
}

/**
 * Format observation time from HHMM to HH:MM
 */
function formatObsTime(obsTime) {
    if (!obsTime) return 'N/A';
    if (obsTime.length !== 4) return obsTime;
    return `${obsTime.substring(0, 2)}:${obsTime.substring(2, 4)}`;
}

/**
 * Handle form submission
 */
function handleFormSubmit(event) {
    event.preventDefault();
    
    const stationId = stationIdInput.value.trim();
    if (!stationId) {
        showError('Please enter a station ID');
        return;
    }
    
    // Show loading indicator and hide other sections
    loadingIndicator.style.display = 'flex';
    resultsContent.style.display = 'none';
    noResults.style.display = 'none';
    errorMessage.style.display = 'none';
    
    // Get form values
    const elementType = elementTypeSelect.value;
    
    // Convert date inputs to YYYYMMDD format for API
    const startDate = startDateInput.value ? startDateInput.value.replace(/-/g, '') : '';
    const endDate = endDateInput.value ? endDateInput.value.replace(/-/g, '') : '';
    
    // Build query parameters
    let queryParams = [];
    if (elementType) queryParams.push(`elementType=${elementType}`);
    if (startDate) queryParams.push(`startDate=${startDate}`);
    if (endDate) queryParams.push(`endDate=${endDate}`);
    
    const queryString = queryParams.length > 0 ? `?${queryParams.join('&')}` : '';
    
    // Call the API
    fetchStationData(stationId, queryString);
}

/**
 * Fetch weather data from API
 */
function fetchStationData(stationId, queryString) {
    const url = `/api/station/${stationId}${queryString}`;
    
    fetch(url)
        .then(response => {
            if (!response.ok) {
                if (response.status === 503) {
                    // Service unavailable - system still initializing
                    throw new Error('System is still initializing. Please wait and try again.');
                }
                throw new Error(`HTTP error: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            loadingIndicator.style.display = 'none';
            
            if (data && data.count > 0 && data.data && data.data.length > 0) {
                displayResults(data);
            } else {
                noResults.style.display = 'block';
            }
        })
        .catch(error => {
            console.error('Error fetching data:', error);
            loadingIndicator.style.display = 'none';
            
            if (error.message.includes('initializing')) {
                // If system is initializing, check status again
                checkSystemStatus();
            } else {
                showError(`Error: ${error.message}`);
            }
        });
}

/**
 * Display error message
 */
function showError(message) {
    errorMessage.querySelector('p').textContent = message;
    errorMessage.style.display = 'block';
}

/**
 * Display weather data results
 */
function displayResults(data) {
    // Set station ID in results header
    resultStationId.textContent = data.stationId;
    
    // Clear previous table results
    resultsBody.innerHTML = '';
    
    // Populate table with data
    data.data.forEach(record => {
        const row = document.createElement('tr');
        
        // Format date from YYYYMMDD to YYYY-MM-DD
        const formattedDate = formatDateForDisplay(record.date);
        
        // Format flags
        const flags = `${record.mFlag || '-'}/${record.qFlag || '-'}/${record.sFlag || '-'}`;
        
        row.innerHTML = `
            <td>${formattedDate}</td>
            <td>${record.element || '-'}</td>
            <td>${record.value || '-'}</td>
            <td>${flags}</td>
            <td>${formatObsTime(record.obsTime)}</td>
        `;
        
        resultsBody.appendChild(row);
    });
    
    // Create chart visualization
    createChart(data.data);
    
    // Show results section
    resultsContent.style.display = 'block';
}

/**
 * Create chart visualization of weather data
 */
function createChart(data) {
    // Destroy previous chart if it exists
    if (chartInstance) {
        chartInstance.destroy();
    }
    
    const chartData = prepareChartData(data);
    
    if (chartData.labels.length > 0) {
        const ctx = weatherChart.getContext('2d');
        
        chartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: chartData.labels,
                datasets: chartData.datasets
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    title: {
                        display: true,
                        text: 'Weather Data Visualization',
                        font: {
                            size: 16
                        }
                    },
                    tooltip: {
                        mode: 'index',
                        intersect: false
                    },
                    legend: {
                        position: 'top'
                    }
                },
                scales: {
                    x: {
                        title: {
                            display: true,
                            text: 'Date'
                        }
                    },
                    y: {
                        title: {
                            display: true,
                            text: 'Value'
                        }
                    }
                }
            }
        });
    } else {
        // Clear chart if no data to display
        const ctx = weatherChart.getContext('2d');
        ctx.clearRect(0, 0, weatherChart.width, weatherChart.height);
        ctx.font = '14px Arial';
        ctx.fillStyle = '#666';
        ctx.textAlign = 'center';
        ctx.fillText('No chart data available', weatherChart.width / 2, weatherChart.height / 2);
    }
}

/**
 * Prepare data for chart visualization
 */
function prepareChartData(data) {
    // Group data by element type and date
    const elementData = {};
    const allDates = new Set();
    
    data.forEach(record => {
        const formattedDate = formatDateForDisplay(record.date);
        allDates.add(formattedDate);
        
        if (!elementData[record.element]) {
            elementData[record.element] = {};
        }
        
        // Convert to number for charting
        const value = parseFloat(record.value);
        if (!isNaN(value)) {
            elementData[record.element][formattedDate] = value;
        }
    });
    
    // Sort dates chronologically
    const sortedDates = Array.from(allDates).sort();
    
    // Create datasets for each element type
    const datasets = [];
    const colors = {
        'PRCP': 'rgba(0, 0, 255, 0.7)',
        'TMAX': 'rgba(255, 0, 0, 0.7)',
        'TMIN': 'rgba(0, 255, 255, 0.7)',
        'SNOW': 'rgba(200, 200, 255, 0.7)',
        'SNWD': 'rgba(230, 230, 255, 0.7)'
    };
    
    // Create chart datasets
    for (const element in elementData) {
        const color = colors[element] || `rgba(${Math.floor(Math.random()*255)}, ${Math.floor(Math.random()*255)}, ${Math.floor(Math.random()*255)}, 0.7)`;
        
        // Map values to dates (using null for missing values)
        const values = sortedDates.map(date => elementData[element][date] || null);
        
        // Get element description
        let elementLabel = element;
        switch (element) {
            case 'PRCP': elementLabel = 'Precipitation'; break;
            case 'TMAX': elementLabel = 'Max Temperature'; break;
            case 'TMIN': elementLabel = 'Min Temperature'; break;
            case 'SNOW': elementLabel = 'Snowfall'; break;
            case 'SNWD': elementLabel = 'Snow Depth'; break;
        }
        
        datasets.push({
            label: elementLabel,
            data: values,
            borderColor: color,
            backgroundColor: color.replace('0.7', '0.1'),
            borderWidth: 2,
            pointRadius: 3,
            pointHoverRadius: 5,
            tension: 0.1,
            fill: false
        });
    }
    
    return {
        labels: sortedDates,
        datasets: datasets
    };
}
