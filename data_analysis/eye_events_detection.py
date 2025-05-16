"""
eye-event detection with the I-DT algorithm, code adapted from 
https://github.com/aeye-lab/pymovements/blob/main/src/pymovements/events/detection/idt.py
"""
from __future__ import annotations
import math
import os
import pandas as pd
import matplotlib.pylab as plt
import matplotlib.collections as mc
import numpy as np
import time
import json



def dispersion(positions:np.ndarray):
    """
    Compute the dispersion of a group of consecutive points in a 2D position time series.

    The dispersion is defined as the sum of the differences between
    the points' maximum and minimum x and y values

    Parameters
    ----------
    positions: array-like
        Continuous 2D position time series.

    Returns
    -------
    dispersion: float
        Dispersion of the group of points.
    """

    # print(np.nanmax(positions, axis=0),np.nanmin(positions, axis=0))
    return sum(np.nanmax(positions, axis=0) - np.nanmin(positions, axis=0))

def check_is_length_matching(**kwargs):
    """Check if two sequences are of equal length.

    Parameters
    ----------
    kwargs
        Keyword argument dictionary with 2 keyword arguments. Both values must be sequences.

    Raises
    ------
    ValueError
        If both sequences are of equal length , or if number of keyword arguments is not 2.
    """

    if len(kwargs) != 2:
        raise ValueError('there must be exactly two keyword arguments in kwargs')
    
    key_1, key_2 = (key for _, key in zip(range(2), kwargs.keys()))
    value_1 = kwargs[key_1]
    value_2 = kwargs[key_2]

    if not len(value_1) == len(value_2):
        raise ValueError(f'The sequences "{key_1}" and "{key_2}" must be of equal length.')


def idt(
        positions: np.ndarray,
        timesteps: np.ndarray,
        minimum_duration: int = 100,
        dispersion_threshold: float = 1.0,
        include_nan: bool = True,
        name: str = 'fixation'):
    """
    Fixation identification based on dispersion threshold.

    The algorithm identifies fixations by grouping consecutive points
    within a maximum separation (dispersion) threshold and a minimum duration threshold.
    The algorithm uses a moving window to check the dispersion of the points in the window.
    If the dispersion is below the threshold, the window represents a fixation,
    and the window is expanded until the dispersion is above threshold.

    The implementation and its default parameter values are based on the description and pseudocode
    from Salvucci and Goldberg :cite:p:`SalvucciGoldberg2000`.

    Parameters
    ----------
    positions: array-like, shape (N, 2)
        Continuous 2D position time series
    timesteps: array-like, shape (N, )
        Corresponding continuous 1D timestep time series. If None, sample based timesteps are
        assumed.
    minimum_duration: int
        Minimum fixation duration. The duration is specified in the units used in ``timesteps``.
         If ``timesteps`` is None, then ``minimum_duration`` is specified in numbers of samples.
    dispersion_threshold: float
        Threshold for dispersion for a group of consecutive samples to be identified as fixation
    include_nan: bool
        Indicator, whether we want to split events on missing/corrupt value (np.nan)
    name:
        Name for detected events in EventDataFrame.

    Returns
    -------
    pl.DataFrame
        A dataframe with detected fixations as rows.

    Raises
    ------
    TypeError
        If minimum_duration is not of type ``int`` or timesteps
    ValueError
        If positions is not shaped (N, 2)
        If dispersion_threshold is not greater than 0
        If duration_threshold is not greater than 0
    """
    positions = np.array(positions)

    timesteps = np.array(timesteps).flatten()

    # Check that timesteps are integers or are floats without a fractional part.
    timesteps_int = timesteps.astype(int)
    if np.any((timesteps - timesteps_int) != 0):
        raise TypeError('timesteps must be of type int')
    timesteps = timesteps_int

    check_is_length_matching(positions=positions, timesteps=timesteps)

    if dispersion_threshold <= 0:
        raise ValueError('dispersion_threshold must be greater than 0')
    if minimum_duration <= 0:
        raise ValueError('minimum_duration must be greater than 0')
    if not isinstance(minimum_duration, int):
        raise TypeError(
            'minimum_duration must be of type int'
            f' but is of type {type(minimum_duration)}',
        )

    onsets = []
    offsets = []
    start_x = []
    start_y = []
    end_x = []
    end_y = []
    avg_x = []
    avg_y = []
    duration = []

    # Infer minimum duration in number of samples.
    timesteps_diff = np.diff(timesteps)
    minimum_sample_duration = int(minimum_duration // np.mean(timesteps_diff))
    print('mean timestep diff', np.mean(timesteps_diff))

    # print(np.mean(timesteps_diff))
    if minimum_sample_duration < 2:
        raise ValueError('minimum_duration must be longer than the equivalent of 2 samples')

    # Initialize window over first points to cover the duration threshold
    win_start = 0
    win_end = minimum_sample_duration

    while win_start < len(timesteps) and win_end <= len(timesteps):

        # Initialize window over first points to cover the duration threshold.
        # This automatically extends the window to the specified minimum event duration.
        win_end = max(win_start + minimum_sample_duration, win_end)
        win_end = min(win_end, len(timesteps))
        if win_end - win_start < minimum_sample_duration:
            break

        if dispersion(positions[win_start:win_end]) <= dispersion_threshold:
            # Add additional points to the window until dispersion > threshold.
            while dispersion(positions[win_start:win_end]) < dispersion_threshold:
                # break if we reach end of input data
                if win_end == len(timesteps):
                    break

                win_end += 1

            # Note a fixation at the centroid of the window points.
            onsets.append(timesteps[win_start])
            offsets.append(timesteps[win_end-2])
            _sx, _sy = positions[win_start]
            _ex, _ey = positions[win_end-2]
            _amp = np.hypot(_sx-_ex, _sy-_ey)
            start_x.append(_sx)
            start_y.append(_sy)
            end_x.append(_ex)
            end_y.append(_ey)
            avg_x.append(np.mean(positions[win_start:win_end-2][:,0]))
            avg_y.append(np.mean(positions[win_start:win_end-2][:,1]))
            duration.append(timesteps[win_end-2] - timesteps[win_start] + 1)

            # Remove window points from points.
            # Initialize new window excluding the previous window
            win_start = win_end
        else:
            # Remove first point from points.
            # Move window start one step further without modifying window end.
            win_start += 1

    # Create proper flat numpy arrays.
    # print(onsets)
    # print(offsets)
    # onsets_arr = np.array(onsets).flatten()
    # offsets_arr = np.array(offsets).flatten()

    event_fix = pd.DataFrame({'onset':onsets, 
                             'offset': offsets,
                             'start_x': start_x,
                             'start_y': start_y,
                             'end_x': end_x,
                             'end_y': end_y,
                             'avg_x': avg_x,
                             'avg_y': avg_y,
                             'duration': duration})
    
    event_sac = pd.DataFrame({'onset':offsets[:-1], 
                             'offset': onsets[1:],
                             'start_x': end_x[:-1],
                             'start_y': end_y[:-1],
                             'end_x': end_x[1:],
                             'end_y': end_y[1:]})

    event_sac['sac_amp'] = np.hypot(event_sac['start_x'] - event_sac['end_x'], 
                                    event_sac['start_y'] - event_sac['end_y'])
    event_sac['duration'] = event_sac['offset'] - event_sac['onset']
   
    return event_fix, event_sac


"""
Calculate the X position of the marker
"""
def tar_pos_x(time_elapsed: float) -> float:
    ampX = 465.0
    freqX = 0.03125
    phaseX = 0.0
    size = 168
    xdpi = 370.70248/2.54
    _x_pos = (ampX - size / 2.) * (np.sin(np.pi * 2. * freqX * time_elapsed + phaseX) + 1.) + 75
    
    return _x_pos/xdpi/35/np.pi*180


"""
Calculate the Y position of the marker
"""
def tar_pos_y(time_elapsed: float) -> float:
    ampY = 1049.5
    freqY = 0.0416666666666667
    phaseY = 0.0
    size = 168
    ydpi = 372.31873/2.54
    _y_pos = (ampY - size / 2.) * (np.sin(np.pi * 2. * freqY * time_elapsed + phaseY) + 1.) + 75
    
    return _y_pos/ydpi/35/np.pi*180


# testing the algorithm with an input file

if __name__ == "__main__":
    
    show_plot = False
    show_vel = False
    save_scanpath = False
    
    # base folder
    base_path = os.path.join(os.getcwd(), 'data')

    # create a folder to save the detected events
    ev_folder = os.path.join(os.getcwd(), 'events')
    if not os.path.exists(ev_folder):
        os.mkdir(ev_folder)

    # create a folder to save the scanpath "images"
    scanpath_folder = os.path.join(os.getcwd(), 'scanpath')
    if not os.path.exists(scanpath_folder):
        os.mkdir(scanpath_folder)
    
    _vel = []; _acc = []; _jrk = []
    
    _subjects = os.listdir(base_path)
    for _subj in _subjects:
        if _subj == ".DS_Store":
            continue
        # subject folder
        for _folder in os.listdir(os.path.join(base_path, _subj)):
            if _folder == ".DS_Store":
                continue
            if not ('validation' in _folder):
                _task = _folder[:2]  # which task, could be fv (free viewing), sp (smooth pursuit),fs (fixation stability)
                _fn = os.path.join(base_path, _subj, _folder, 'MobileEyeTrackingRecord.csv')
                print('Processing...', _subj, _folder)

                # get the task timestamps, i.e., task start and end time
                _task_fn = os.path.join(base_path, _subj, _folder, _folder[:2] + '_timestamps.json')
                with open(_task_fn) as _tmp_json:
                    _json = json.load(_tmp_json)
                    if _task == 'fv':
                        _task_start = np.int64(_json['normalFixationShowTimeStampList'][0]/1000000)
                    else:
                        _task_start = np.int64(_json['markerMotionStartTimeStamp']/1000000)
                    _task_end = np.int64(_json['normalTrialEndTimeStamp']/1000000)
                    print(_task, _task_end, _task_start, _task_end-_task_start)

                # put the results of a single subject into pandas data frames
                _mix = pd.read_csv(_fn,  engine='python', na_values='Nan')
                xdpi = 370.70248/2.54
                ydpi = 372.31873/2.54

                # converting to degree of visual angles
                # _mix['x_raw'] = _mix['x_raw']/xdpi/35/np.pi*180
                # _mix['y_raw'] = _mix['y_raw']/ydpi/35/np.pi*180
                _mix['x_deg'] = _mix['x_filtered']/xdpi/35/np.pi*180
                _mix['y_deg'] = _mix['y_filtered']/ydpi/35/np.pi*180

                # gaze timestamps in msd
                _mix['gt_ms'] = np.int64((_mix['gaze_timestamp'])/1000000)

                # check if the length of the samples is sufficient for event detection
                if len(_mix['gt_ms']) <=2:
                    continue

                # event detection
                # set to 120 because subj 10040 had a low sample rate 20Hz
                _minimum_fix_duration = 120
                # if _subj in ['10040', '10061', '10060', '10094', '10072']:
                #     _minimum_fix_duration = 120

                #  x_raw       y_raw  x_filtered  y_filtered   gaze_timestamp  record_timestamp
                _fix, _sac = idt(_mix[['x_deg', 'y_deg']],
                                _mix['gt_ms'],
                                minimum_duration = _minimum_fix_duration,
                                dispersion_threshold = 1.0  # 1.0, set to 1.5 to accommodate the overall accuracy
                                )

                # using the task start and end timestamps to filter the detected eye events
                # selecting rows based on condition 
                _fix = _fix[(_fix['offset'] > _task_start) & (_fix['onset'] < _task_end)] 
                _sac = _sac[(_sac['onset'] >= _task_start) & (_sac['offset'] <= _task_end)] 

                # get the timestamps relative to task onsets
                _fix['onset'] = _fix['onset'] - _task_start
                _fix['offset'] = _fix['offset'] - _task_start
                _sac['onset'] = _sac['onset'] - _task_start
                _sac['offset'] = _sac['offset'] - _task_start

                # print out the detection results
                # print(_fix)
                # print(_sac)

                # save the detected events into CSV
                _ev_sac_path = os.path.join(ev_folder, 'sac_' + _subj + '_' + _task + '.csv') 
                _ev_fix_path = os.path.join(ev_folder, 'fix_' + _subj + '_' + _task + '.csv') 
                _fix.to_csv(_ev_fix_path, index=False)
                _sac.to_csv(_ev_sac_path, index=False)

                # data for plotting and smooth pursuit offset calculation
                _mix = _mix[(_mix['gt_ms'] >= _task_start) & (_mix['gt_ms'] < _task_end)] 
                _mix['gt_ms'] = _mix['gt_ms'] - _task_start
                
                # check the sample intervals, as we need to assume equal intevals
                # for velocity, acceleration, and jerk calculation, when using np.gradient()
                # _timesteps_diff = np.diff(_mix['gt_ms'])
                # print(np.median(_timesteps_diff), np.std(_timesteps_diff))
                # plt.hist(_timesteps_diff)
                # plt.show()
                _mix['x_vel'] = np.gradient(_mix['x_deg'], _mix['gt_ms']/1000.)
                _mix['y_vel'] = np.gradient(_mix['y_deg'], _mix['gt_ms']/1000.)
                _mix['x_acc'] = np.gradient(_mix['x_vel'], _mix['gt_ms']/1000.)
                _mix['y_acc'] = np.gradient(_mix['y_vel'], _mix['gt_ms']/1000.)
                _mix['x_jrk'] = np.gradient(_mix['x_acc'], _mix['gt_ms']/1000.)
                _mix['y_jrk'] = np.gradient(_mix['y_acc'], _mix['gt_ms']/1000.)
                
                _mix['s_vel'] = np.hypot(_mix['x_vel'], _mix['y_vel'])
                _mix['s_acc'] = np.hypot(_mix['x_acc'], _mix['y_acc'])
                _mix['s_jrk'] = np.hypot(_mix['x_jrk'], _mix['y_jrk'])
                
                _vel.extend([np.max(_mix['s_vel']), np.min(_mix['s_vel'])])
                _acc.extend([np.max(_mix['s_acc']), np.min(_mix['s_acc'])])
                _jrk.extend([np.max(_mix['s_jrk']), np.min(_mix['s_jrk'])])

                # in the smooth pursuit task, we calculate the x, y offsets
                if _task == 'sp':
                    # get the target location for each sample
                    _mix['tar_x'] = tar_pos_x(_mix['gt_ms']/1000.)
                    _mix['tar_y'] = tar_pos_y(_mix['gt_ms']/1000.)
                    # calculate the x, y offsets
                    _mix['offset_x'] = _mix['x_deg'] - _mix['tar_x']
                    _mix['offset_y'] = _mix['y_deg'] - _mix['tar_y']
                    _sp = pd.DataFrame({'offset_x_mean': [np.mean(_mix['offset_x'])],
                                        'offset_x_std': [np.std(_mix['offset_x'])],
                                        'offset_y_mean': [np.mean(_mix['offset_y'])],
                                        'offset_y_std': [np.std(_mix['offset_y'])]})
                    # save smooth pursuit features to file
                    _ev_sp_path = os.path.join(ev_folder, 'sp_' + _subj + '_' + _task + '.csv')
                    _sp.to_csv(_ev_sp_path, index=False)
                
                if show_plot:
                # plot the gaze trace
                    if _task=='sp':  # plot the x, y offset for the smooth pursuit task
                        _mix.plot(x='gt_ms', y=['x_deg', 'y_deg', 'tar_x', 'tar_y']) #, 'offset_x', 'offset_y'])
                    else:
                        # show gaze traces with / without velocity traces
                        if show_vel:
                            _mix.plot(x='gt_ms', y=['x_deg', 'y_deg', 'x_vel', 'y_vel'])
                        else:
                            _mix.plot(x='gt_ms', y=['x_deg', 'y_deg'])
                                        
                    # plot eye events (saccades and fixations)
                    if _task in ['fv', 'fs']: # skip fixation and saccade events plotting in the smooth pursuit task
                        for _idx, _row in _fix.iterrows():
                            x0 = _row['onset']
                            x1 = _row['offset']
                            plt.axvspan(x0, x1, color='blue', alpha=0.1, lw=0)
                    
                    plt.show()

                # plt.scatter(_sac['sac_amp'], _sac['sac_amp']/(_sac['duration']/1000))
                # plt.show()

                if _task == 'fv':
                    # save scanpath for data mining
                    if save_scanpath:
                        _line_coords = []
                        _line_colors = []
                        for _row in range(1, len( _mix['gt_ms'])):
                            # grab the coords of two consecutive samples
                            _start_x = _mix.iloc[_row-1]['x_deg']
                            _start_y = _mix.iloc[_row-1]['y_deg']
                            _end_x = _mix.iloc[_row]['x_deg']
                            _end_y = _mix.iloc[_row]['y_deg']
                            # set the line color for a sample interval, normalize to the maximum in all data files
                            # set a threshold for drawing to avoid clustering
                            _color_threshold = 120/256.0
                            _smp_vel = _mix.iloc[_row-1]['s_vel'] / 333.0
                            _smp_acc = _mix.iloc[_row-1]['s_acc'] / 5913.0
                            _smp_jrk = _mix.iloc[_row-1]['s_jrk'] / 170866
                            if _smp_vel < _color_threshold:
                                _smp_vel = 0
                            if _smp_acc < _color_threshold:
                                _smp_acc = 0
                            if _smp_jrk < _color_threshold:
                                _smp_jrk = 0
                            
                            _line_coords.append([(_start_x, _start_y), (_end_x, _end_y)])
                            _line_colors.append((_smp_acc, _smp_vel, _smp_jrk))

                        # plot the scan path with matplotlib line collections
                        _path = mc.LineCollection(_line_coords, colors=_line_colors, linewidth=3)
                        plt.style.use("dark_background")
                        fig, ax = plt.subplots()
                        ax.add_collection(_path)
                        ax.set_xlim([0,12.11])  # 1080 pixel
                        ax.set_ylim([0, 25.24])  # 2250 pixel
                        ax.set_aspect('equal')
                        ax.set_axis_off()
                        fig.set_size_inches([12.11/3, 25.24/3])

                        _img_path = os.path.join(scanpath_folder, 'scanpath_' + _subj + '.jpg')
                        plt.savefig(_img_path)
                        
    # print(np.max(_acc), np.min(_acc), np.max(_vel), np.min(_vel), np.max(_jrk), np.min(_jrk))